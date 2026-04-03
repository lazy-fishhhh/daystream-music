package com.future.music.service.impl;

import com.future.login.exception.BusinessException;
import entity.dto.FileUploadDTO;
import com.future.music.service.IFileService;
import com.future.music.service.IMinIoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
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

    // Operation常量
    private static final String OPERATION_UPLOAD = "upload";
    private static final String OPERATION_MERGE = "merge";
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private IMinIoService minIoService;

    @Override
    public boolean check(String fileMd5, String chunk, String chunkMd5) {
        log.info("=====================================");
        log.info("==== 检查分片 START =====");
        log.info("传入 fileMd5: {}", fileMd5);
        log.info("传入 chunk: {}", chunk);
        log.info("传入 chunkMd5: {}", chunkMd5);

        // 打印 Redis 里这个 key 到底有没有！！！
        Map<Object, Object> allEntries = redisTemplate.opsForHash().entries(fileMd5);
        log.info("Redis 中 key=[{}] 对应的全部数据: {}", fileMd5, allEntries);
        log.info("Redis 数据是否为空: {}", allEntries.isEmpty());

        // 开始检查
        Object o = redisTemplate.opsForHash().get(fileMd5, REDIS_CHUNK_MD5_PREFIX + chunk);
        log.info("获取到的 Redis MD5 值: {}", o);

        if (o == null) {
            log.error("❌ 检查失败：Redis 中确实没有这条 MD5 记录");
            return false;
        }

        String storedChunkMd5 = o.toString().replace("\"", "").trim();
        log.info("去掉引号后: {}", storedChunkMd5);

        if (!chunkMd5.equals(storedChunkMd5)) {
            log.error("❌ MD5 不匹配");
            return false;
        }

        log.info("==== 检查分片 END =====");
        log.info("=====================================");
        return true;
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
        return upload(md5Value, chunkIndex, start, end, chunks, fileSize, chunkMd5, multipartFile);
    }

    /**
     * 分片上传实现方法（直接接受参数）
     */
    @Override
    public String upload(String md5Value, String chunkIndex, String start, String end,
                         String chunks, String fileSize, String chunkMd5, MultipartFile multipartFile) throws IOException {

        // 使用MinIO存储分片文件 【0-based 索引】
        String chunkPath = MINIO_CHUNKS_PREFIX + chunkMd5 + "_" + chunkIndex;
        try {
            // 上传分片到MinIO
            String storedPath = minIoService.uploadChunkFile(multipartFile, chunkPath);


            // 直接使用前端传入的 chunkIndex（0、1、2...），不修改、不偏移
            String finalChunkIndex = chunkIndex;

            // 分片信息存入Redis，直接使用 0-based 索引
            redisTemplate.opsForHash().put(md5Value, REDIS_CHUNK_LOCATION_PREFIX + finalChunkIndex, storedPath);
            redisTemplate.opsForHash().put(md5Value, REDIS_CHUNK_START_END_PREFIX + finalChunkIndex, start + "_" + end);
            redisTemplate.opsForHash().put(md5Value, REDIS_CHUNK_MD5_PREFIX + finalChunkIndex, chunkMd5);

            // 只在上传 0号分片（第一个分片）时存储文件基本信息
            if ("0".equals(chunkIndex)) {
                redisTemplate.opsForHash().put(md5Value, REDIS_FILE_SIZE, fileSize);
                redisTemplate.opsForHash().put(md5Value, REDIS_FILE_CHUNKS, chunks);
            }

            log.info("分片信息已存入Redis，md5Value={}, chunkIndex={}, storedPath={}", md5Value, chunkIndex, storedPath);
            log.info("Redis中当前的所有键值对: {}", redisTemplate.opsForHash().entries(md5Value));
            return "分片上传成功";
        } catch (RuntimeException e) {
            log.error("分片 {} 上传到MinIO失败: {}", chunkIndex, e.getMessage(), e);
            throw new IOException("分片上传失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("分片 {} 上传过程中发生未知错误: {}", chunkIndex, e.getMessage(), e);
            throw new IOException("分片上传失败：" + e.getMessage());
        }
    }

    @Override
    public String merge(HttpServletRequest request) throws IOException {
        log.info("合并分片...");
        Map<String, String> requestParam = this.doRequestParam(request);
        String md5Value = requestParam.get("md5Value");
        String originalFilename = requestParam.get("originalFilename");
        return merge(md5Value, originalFilename);
    }


    @Override
    public String merge(String md5Value, String originalFilename) throws IOException {


        // 校验所有切片是否己经上传完毕
        boolean flag = this.checkBeforeMerge(md5Value);
        if (!flag) {
            throw new IOException("切片未完全上传");
        }

        //检查是否已经有相同md5值的文件上传；主要是对名字不同，而实际文件相同的文件，直接返回成功
        Object file_location = redisTemplate.opsForHash().get(md5Value, REDIS_FILE_LOCATION);
        if (file_location != null) {
            return file_location.toString();
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
            // 前端使用0-based索引（分片0, 1, 2...）
            for (int i = 0; i < file_chunks; i++) {
                String redisKey = REDIS_CHUNK_LOCATION_PREFIX + i;
                Object chunkPathObj = redisTemplate.opsForHash().get(md5Value, redisKey);
                log.info("合并分片 {}，Redis键: {}，值: {}", i, redisKey, chunkPathObj);
                if (chunkPathObj == null) {
                    // 打印所有Redis键值对，帮助调试
                    Map<Object, Object> allEntries = redisTemplate.opsForHash().entries(md5Value);
                    log.error("分片 {} 路径信息缺失，Redis中所有键值对: {}", i, allEntries);
                    throw new IOException("分片 " + i + " 路径信息缺失");
                }
                String chunkPath = chunkPathObj.toString();
                if (chunkPath.isEmpty()) {
                    throw new IOException("分片 " + i + " 路径信息为空");
                }

                // 从URL中提取MinIO对象名称
                String objectName = extractObjectNameFromUrl(chunkPath);
                log.info("分片 {}，原始路径: {}，提取的对象名称: {}", i, chunkPath, objectName);
                if (objectName == null) {
                    throw new IOException("分片 " + i + " 对象名称提取失败，路径: " + chunkPath);
                }
                if (objectName.isEmpty()) {
                    throw new IOException("分片 " + i + " 对象名称为空，路径: " + chunkPath);
                }
                
                log.debug("处理分片 {}，对象名称: {}", i, objectName);
                
                // 从MinIO获取分片文件的输入流，添加重试机制
                // 从 MinIO 下载单个分片文件的字节流，供写入本地临时文件使用
                int retryCount = 0;
                int maxRetries = 3;
                while (retryCount < maxRetries) {
                    try {
                        inputStream = minIoService.getObjectStream(objectName);
                        if (inputStream != null) {
                            break;
                        }
                    } catch (Exception e) {
                        log.warn("获取分片 {} 文件流失败，正在重试... ({}/{})\n{}", i, retryCount + 1, maxRetries, e.getMessage());
                    }
                    retryCount++;
                    try {
                        Thread.sleep(1000); // 等待1秒后重试
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                if (inputStream == null) {
                    throw new IOException("获取分片 " + i + " 文件流失败: The specified key does not exist");
                }

                byte[] bytes = new byte[1024 * 1024];
                int len;
                // 写入服务器本地临时文件
                while ((len = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, len);
                }

                inputStream.close();
                inputStream = null;
                log.debug("分片 {} 处理完成", i);
            }
        } catch (IOException e) {
            log.error("合并分片失败: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("关闭输入流失败", e);
                }
            }
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

        return filePath;
    }


    /**
     * 从URL中提取MinIO对象名称
     * 自动适配任意域名/桶名/前缀，只提取 chunks/ 开头的真实路径
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // 1. 清理格式：去掉双引号、首尾空格
        String cleanUrl = url.replace("\"", "").trim();

        try {
            // 2. 如果是完整HTTP链接，只提取 路径部分（丢弃域名/端口）
            if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                java.net.URL uri = new java.net.URL(cleanUrl);
                cleanUrl = uri.getPath();
            }

            // 3. 通用核心：查找 chunks/ 的位置（唯一业务固定目录）
            int chunksIndex = cleanUrl.indexOf("chunks/");
            if (chunksIndex != -1) {
                // 直接从 chunks/ 开始返回，前面所有冗余路径全部丢弃
                return cleanUrl.substring(chunksIndex);
            }

        } catch (Exception e) {
            log.warn("URL解析异常，原始URL：{}", url, e);
        }

        // 兜底：没有找到chunks，直接返回清理后的路径
        return cleanUrl;
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

    @Override
    public String uploadLargeFile(FileUploadDTO fileUploadDTO) {
        try {
            // 从FileUploadDTO中获取必要信息
            String md5Value = fileUploadDTO.getMd5Value();
            String chunk = fileUploadDTO.getChunk();
            String chunks = fileUploadDTO.getChunks();
            String chunkMd5 = fileUploadDTO.getChunkMd5();
            String start = fileUploadDTO.getStart();
            String end = fileUploadDTO.getEnd();
            String fileSize = fileUploadDTO.getSize();
            String originalFilename = fileUploadDTO.getOriginalFilename();
            MultipartFile multipartFile = fileUploadDTO.getFile();
            String operation = fileUploadDTO.getOperation();

            log.info("开始大文件上传，md5Value={}, chunk={}, chunks={}, operation={}, originalFilename={}", 
                    md5Value, chunk, chunks, operation, originalFilename);

            // 验证必要参数
            if (md5Value == null || md5Value.isEmpty()) {
                log.error("参数验证失败：md5Value为空");
                throw new IllegalArgumentException("文件MD5值不能为空");
            }
            if (chunk == null || chunk.isEmpty()) {
                log.error("参数验证失败：chunk为空");
                throw new IllegalArgumentException("分片索引不能为空");
            }
            if (chunks == null || chunks.isEmpty()) {
                log.error("参数验证失败：chunks为空");
                throw new IllegalArgumentException("总分片数不能为空");
            }
            if (chunkMd5 == null || chunkMd5.isEmpty()) {
                log.error("参数验证失败：chunkMd5为空");
                throw new IllegalArgumentException("分片MD5值不能为空");
            }
            if (multipartFile == null || multipartFile.isEmpty()) {
                log.error("参数验证失败：multipartFile为空");
                throw new IllegalArgumentException("上传文件不能为空");
            }
            if (operation == null || operation.isEmpty()) {
                log.error("参数验证失败：operation为空");
                throw new IllegalArgumentException("操作类型不能为空");
            }

            // 如果是上传分片操作
            if (OPERATION_UPLOAD.equals(operation)) {
                log.info("执行分片上传操作，chunk={}/{}, md5Value={}", chunk, chunks, md5Value);
                // 检查分片是否已存在
                boolean isChunkExists = check(md5Value, chunk, chunkMd5);
                // 如果分片已存在，直接返回成功
                if (isChunkExists) {
                    log.info("分片已存在，跳过上传，chunk={}", chunk);
                    return "分片已存在";
                }
                // 上传新分片
                String uploadResult = upload(md5Value, chunk, start, end, chunks, fileSize, chunkMd5, multipartFile);
                // 检查是否上传成功
                if (uploadResult.contains("失败")) {
                    log.error("分片上传失败，chunk={}, result={}", chunk, uploadResult);
                    return uploadResult;
                }
                log.info("分片上传成功，chunk={}/{}, md5Value={}", chunk, chunks, md5Value);
            } else if (OPERATION_MERGE.equals(operation)) {
                log.info("执行分片合并操作，md5Value={}, originalFilename={}", md5Value, originalFilename);
                // 直接执行合并操作
                return merge(md5Value, originalFilename);
            }

            // 如果是最后一个分片，则自动合并
            // 前端使用0-based索引（分片0, 1, 2...），所以最后一个分片是 chunk == chunks - 1
            int currentChunk = Integer.parseInt(chunk);
            int totalChunks = Integer.parseInt(chunks);
            if (currentChunk == totalChunks - 1) {
                log.info("最后一个分片上传完成，自动执行合并操作，chunk={}/{}, md5Value={}", chunk, chunks, md5Value);
                // 最后一个分片上传完成，执行合并操作
                return merge(md5Value, originalFilename);
            }

            log.info("大文件上传操作成功，chunk={}/{}, md5Value={}", chunk, chunks, md5Value);
            return "操作成功";
        } catch (NumberFormatException e) {
            log.error("参数格式错误: {}", e.getMessage(), e);
            throw new IllegalArgumentException("参数格式错误: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("参数验证失败: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("大文件上传异常: {}", e.getMessage(), e);
            throw new RuntimeException("大文件上传失败: " + e.getMessage(), e);
        }
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
            if (hashKey.toString().startsWith(REDIS_CHUNK_LOCATION_PREFIX)) {
                String chunkPath = map.get(hashKey).toString();
                // 这里统一提取真实路径，修复删除失败问题
                String realObjectName = extractObjectNameFromUrl(chunkPath);
                chunkPaths.add(realObjectName);
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
    /**
     * 合并分片之前的【预检方法】
     * 作用：检查当前文件的【所有分片】是否都已经上传完成、有效、可合并
     * 返回 true = 可以合并
     * 返回 false = 不能合并（缺片/文件丢失/信息不完整）
     */
    private boolean checkBeforeMerge(String key) {
        // 1. 获取当前文件 MD5 对应的【所有 Redis 哈希数据】
        Map map = redisTemplate.opsForHash().entries(key);

        // 2. 获取【总分片数】（比如 3）
        Object file_chunks = map.get(REDIS_FILE_CHUNKS);
        // 如果连总分片数都没有 → 信息缺失 → 不能合并
        if (file_chunks == null) {
            log.warn("文件分片总数信息缺失，key: {}", key);
            return false;
        }

        // 把总分片数转成 int
        int totalChunks = Integer.parseInt(file_chunks.toString());

        // 三个统计计数器
        int md5Count = 0;         // 计数：有多少个分片存了 MD5
        int locationCount = 0;    // 计数：有多少个分片存了路径
        int fileExistsCount = 0;  // 计数：有多少个分片【真实存在于 MinIO】

        // 3. 遍历 Redis 里所有的字段（chunk_md5_0、chunk_location_1...）
        for (Object hashKey : map.keySet()) {
            String keyStr = hashKey.toString();

            // 如果这个key是分片MD5 → 计数+1
            if (keyStr.startsWith(REDIS_CHUNK_MD5_PREFIX)) {
                md5Count++;
            }

            // 如果这个key是分片路径 → 检查路径是否有效、文件是否真实存在
            if (keyStr.startsWith(REDIS_CHUNK_LOCATION_PREFIX)) {
                Object location = map.get(hashKey);

                // 路径不为空 → 计数+1
                if (location != null && !location.toString().isEmpty()) {
                    locationCount++;

                    // 从路径中解析出 MinIO 中的真实文件名
                    String chunkPath = location.toString();
                    String objectName = extractObjectNameFromUrl(chunkPath);

                    // 检查这个分片文件【真的在 MinIO 里存在】
                    if (objectName != null && !objectName.isEmpty()) {
                        if (minIoService.isMvObjectExists(objectName)) {
                            // 真实存在 → 有效分片计数+1
                            fileExistsCount++;
                        } else {
                            // Redis里有记录，但MinIO里文件没了 → 异常
                            log.warn("分片文件在MinIO中不存在: {}", objectName);
                        }
                    }
                }
            }
        }

        // 打印最终检查结果
        log.info("合并前检查：totalChunks={}, md5Count={}, locationCount={}, fileExistsCount={}",
                totalChunks, md5Count, locationCount, fileExistsCount);

        // ✅ 最终判断条件（必须全部满足才能合并）
        // 1. 分片MD5数量 = 总分片数
        // 2. 分片路径数量 = 总分片数
        // 3. MinIO真实存在的分片 = 总分片数
        boolean result = totalChunks == md5Count
                && totalChunks == locationCount
                && totalChunks == fileExistsCount;

        if (!result) {
            log.warn("分片信息不完整或文件不存在，无法合并");
        }

        return result;
    }
}