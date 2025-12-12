package com.future.music.service.impl;

import com.future.music.service.IFileService;
import com.future.music.service.IMinIoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * 文件服务实现类
 * 实现分片上传、合并、下载等文件处理相关方法
 *
 * @author future
 * @date 2023-09-01
 */
@Service
@Slf4j
public class FileServiceImpl implements IFileService {
    // Redis键前缀常量
    private static final String REDIS_CHUNK_MD5_PREFIX = "chunk_md5_";
    private static final String REDIS_CHUNK_LOCATION_PREFIX = "chunk_location_";
    private static final String REDIS_CHUNK_START_END_PREFIX = "chunk_start_end_";
    private static final String REDIS_FILE_SIZE = "file_size";
    private static final String REDIS_FILE_CHUNKS = "file_chunks";
    private static final String REDIS_FILE_LOCATION = "file_location";
    
    // MinIO路径常量
    private static final String MINIO_CHUNKS_PREFIX = "chunks/";
    private static final String MINIO_MERGED_PREFIX = "merged/";
    
    // 其他常量
    private static final String TEMP_FILE_PREFIX = "merge";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final String FILE_CONTENT_TYPE = "application/octet-stream";
    
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private IMinIoService minIoService;

    @Override
    public boolean check(String fileMd5, String chunk, String chunkMd5) {
        Object o = redisTemplate.opsForHash().get(fileMd5, REDIS_CHUNK_MD5_PREFIX + chunk);
        return chunkMd5.equals(o);
    }

    @Override
    public String upload(HttpServletRequest request, MultipartFile multipartFile) throws IOException {
        log.info("分片上传....");
        Map<String, String> requestParam = this.doRequestParam(request);
        String md5Value = requestParam.get("md5Value");//整体文件的md5值
        String chunkIndex = requestParam.get("chunk");//分片在所有分片文件中索引位置
        String start = requestParam.get("start");//当前分片在整个数据文件中的开始位置
        String end = requestParam.get("end");//当前分片在整个数据文件中的结束位置
        String chunks = requestParam.get("chunks");//整体文件总共被分了多少片
        String fileSize = requestParam.get("size");//整体文件大小
        String chunkMd5 = requestParam.get("chunkMd5");//分片文件的md5值

        // 使用MinIO存储分片文件
        String chunkPath = MINIO_CHUNKS_PREFIX + chunkMd5 + "_" + chunkIndex;
        try {
            // 上传分片到MinIO
            // 这里我们自己指定带有index的存储路径，方便检索。
            // 所以自定义一个uploadChunkFile，不用原来自动生成路径名的uploadFile了
            String storedPath = minIoService.uploadChunkFile(multipartFile, chunkPath);

            // 分片信息存入Redis
            Map<String, String> map = new HashMap<>();
            map.put(REDIS_CHUNK_LOCATION_PREFIX + chunkIndex, storedPath);//分片在MinIO中的存储路径
            map.put(REDIS_CHUNK_START_END_PREFIX + chunkIndex, start + "_" + end);
            map.put(REDIS_FILE_SIZE, fileSize);
            map.put(REDIS_FILE_CHUNKS, chunks);
            map.put(REDIS_CHUNK_MD5_PREFIX + chunkIndex, chunkMd5);

            // 缓存键名为文件的md5值
            redisTemplate.opsForHash().putAll(md5Value, map);
            return "分片上传成功";
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IOException("分片上传失败：" + e.getMessage());
        }
    }

    @Override
    public String merge(HttpServletRequest request) throws IOException {
        log.info("合并分片...");
        Map<String, String> requestParam = this.doRequestParam(request);
        String md5Value = requestParam.get("md5Value");
        String originalFilename = requestParam.get("originalFilename");
        // 校验所有切片是否己经上传完毕
        boolean flag = this.checkBeforeMerge(md5Value);
        if (!flag) {
            throw new IOException("切片未完全上传");
        }

        //检查是否已经有相同md5值的文件上传；主要是对名字不同，而实际文件相同的文件，直接返回成功
        Object file_location = redisTemplate.opsForHash().get(md5Value, REDIS_FILE_LOCATION);
        if (file_location != null) {
            return "success";
        }

        //这里要特别注意，合并分片的时候一定要按照分片的索引顺序进行合并，否则文件无法使用；
        int file_chunks = Integer.parseInt(redisTemplate.opsForHash().get(md5Value, REDIS_FILE_CHUNKS).toString());

        // 使用临时文件来合并分片
        // 创建临时文件（服务器本地磁盘）
        // 把 MinIO 的分片文件，按顺序写入到本地临时文件中，完成分片合并
        File tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        OutputStream outputStream = new FileOutputStream(tempFile);
        InputStream inputStream = null;

        try {
            for (int i = 0; i < file_chunks; i++) {
                String chunkPath = redisTemplate.opsForHash().get(md5Value, REDIS_CHUNK_LOCATION_PREFIX + i).toString();
                // 从MinIO获取分片文件的输入流
                // 从 MinIO 下载单个分片文件的字节流，供写入本地临时文件使用
                inputStream = minIoService.getObjectStream(chunkPath);

                byte[] bytes = new byte[1024 * 1024];
                int len;
                // 写入服务器本地临时文件
                while ((len = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, len);
                }

                inputStream.close();
            }
        } finally {
            outputStream.close();
        }

        // 把合并好的服务器本地临时文件上传到 MinIO，作为最终的完整文件。
        String fileDir = MINIO_MERGED_PREFIX + md5Value.substring(0, 2) + "/" + md5Value.substring(2, 4) + "/";
        String filePath = fileDir + originalFilename;

        // 使用MinIO客户端直接上传文件
        try (FileInputStream tempInputStream = new FileInputStream(tempFile)) {
            filePath = minIoService.uploadFileStream(tempInputStream, filePath, FILE_CONTENT_TYPE);
        }

        // 更新Redis中的文件位置信息
        redisTemplate.opsForHash().put(md5Value, REDIS_FILE_LOCATION, filePath);

        // 删除临时文件
        tempFile.delete();

        // 删除MinIO中的分片文件
        this.delTmpFile(md5Value);

        return "分片合并成功";
    }

    @Override
    public void download(String fileName, HttpServletResponse response) throws IOException {
        response.setContentType(FILE_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        // 从Redis中获取文件的存储路径
        String md5Value = null;
        // 这里假设fileName是原始文件名，需要根据实际情况调整
        // 如果fileName是文件的MD5值，可以直接使用
        // md5Value = fileName;

        // 如果fileName是原始文件名，需要通过其他方式获取MD5值
        // 这里简化处理，直接使用原始文件名作为MinIO中的对象名
        String objectName = MINIO_MERGED_PREFIX + fileName;

        // 从MinIO下载文件
        minIoService.downloadFile(objectName, response);
    }

    /**
     * 删除临时文件和分片文件
     *
     * @param md5Value 文件MD5值
     * @throws IOException IO异常
     */
    private void delTmpFile(String md5Value) throws IOException {
        Map map = redisTemplate.opsForHash().entries(md5Value);
        List<String> list = new ArrayList<>();
        List<String> chunkPaths = new ArrayList<>();

        for (Object hashKey : map.keySet()) {
            if (hashKey.toString().startsWith(REDIS_CHUNK_LOCATION_PREFIX.substring(0, REDIS_CHUNK_LOCATION_PREFIX.length() - 1))) {
                String chunkPath = map.get(hashKey).toString();
                chunkPaths.add(chunkPath);
                list.add(hashKey.toString());
            }
            if (hashKey.toString().startsWith(REDIS_CHUNK_START_END_PREFIX)) {
                list.add(hashKey.toString());
            }
            if (hashKey.toString().startsWith(REDIS_CHUNK_MD5_PREFIX)) {
                list.add(hashKey.toString());
            }
        }

        // 批量删除MinIO中的分片文件
        if (!chunkPaths.isEmpty()) {
            boolean flag = minIoService.batchDeleteFiles(chunkPaths);
            log.info("批量删除分片文件结果: " + flag);
        }

        list.add(REDIS_FILE_CHUNKS);
        list.add(REDIS_FILE_SIZE);
        redisTemplate.opsForHash().delete(md5Value, list.toArray());
    }

    /**
     * 处理请求参数
     *
     * @param request HttpServletRequest
     * @return 请求参数Map
     */
    private Map<String, String> doRequestParam(HttpServletRequest request) {
        Map<String, String> requestParam = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            requestParam.put(paramName, paramValue);
            log.info(paramName + "：" + paramValue);
        }
        log.info("----------------------------");
        return requestParam;
    }

    /**
     * 合并分片前检验文件整体的所有分片是否全部上传
     *
     * @param key 文件MD5值
     * @return 是否全部上传
     */
    private boolean checkBeforeMerge(String key) {
        Map map = redisTemplate.opsForHash().entries(key);
        Object file_chunks = map.get(REDIS_FILE_CHUNKS);
        int i = 0;
        // 统计文件的Redis中的分片数量
        for (Object hashKey : map.keySet()) {
            if (hashKey.toString().startsWith(REDIS_CHUNK_MD5_PREFIX)) {
                ++i;
            }
        }

        // 判断Redis中的 分片个数 是否与 总数 相等
        if (Integer.valueOf(file_chunks.toString()) == (i)) {
            return true;
        }
        return false;
    }
}