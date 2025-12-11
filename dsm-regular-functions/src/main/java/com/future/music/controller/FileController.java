//package com.future.music.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.future.music.service.IMinIoService;
//import jakarta.annotation.Resource;
//import jakarta.servlet.ServletOutputStream;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.*;
//import java.net.URLEncoder;
//import java.util.*;
//
//@RestController
//@RequestMapping("/file")
//@Slf4j
//public class FileController {
//    @Resource
//    private RedisTemplate redisTemplate;
//
//    @Resource
//    private IMinIoService minIoService;
//    /**
//     * 检验分片文件是否已经上传到MinIO
//     *
//     * @param fileMd5  整体文件md5值
//     * @param chunk    当前上传分片在所有分片文件中索引位置
//     * @param chunkMd5 分片文件的md5值
//     * @return 是否已经上传
//     */
//    @PostMapping("/check")
//    public boolean check(String fileMd5, String chunk, String chunkMd5) {
//        Object o = redisTemplate.opsForHash().get(fileMd5, "chunk_md5_" + chunk);
//        return chunkMd5.equals(o);
//    }
//
//    /**
//     * 分片上传接口
//     *
//     * @param request
//     * @param multipartFile
//     * @return
//     * @throws IOException
//     */
//    @PostMapping("/upload")
//    public String upload(HttpServletRequest request, MultipartFile multipartFile) {
//        log.info("分片上传....");
//        Map<String, String> requestParam = this.doRequestParam(request);
//        String md5Value = requestParam.get("md5Value");//整体文件的md5值
//        String chunkIndex = requestParam.get("chunk");//分片在所有分片文件中索引位置
//        String start = requestParam.get("start");//当前分片在整个数据文件中的开始位置
//        String end = requestParam.get("end");//当前分片在整个数据文件中的结束位置
//        String chunks = requestParam.get("chunks");//整体文件总共被分了多少片
//        String fileSize = requestParam.get("size");//整体文件大小
//        String chunkMd5 = requestParam.get("chunkMd5");//分片文件的md5值
//
//        // 使用MinIO存储分片文件
//        String chunkPath = "chunks/" + chunkMd5 + "_" + chunkIndex;
//        try {
//            // 上传分片到MinIO
//            String storedPath = minIoService.uploadChunkFile(multipartFile, chunkPath);
//
//            Map<String, String> map = new HashMap<>();
//            map.put("chunk_location_" + chunkIndex, storedPath);//分片在MinIO中的存储路径
//            map.put("chunk_start_end_" + chunkIndex, start + "_" + end);
//            map.put("file_size", fileSize);
//            map.put("file_chunks", chunks);
//            map.put("chunk_md5_" + chunkIndex, chunkMd5);
//            redisTemplate.opsForHash().putAll(md5Value, map);
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        }
//
//        return "success";
//    }
//
//    /**
//     * 合并分片文件接口
//     *
//     * @param request
//     * @return
//     * @throws IOException
//     */
//    @PostMapping("/merge")
//    public String merge(HttpServletRequest request) throws IOException {
//        log.info("合并分片...");
//        Map<String, String> requestParam = this.doRequestParam(request);
//        String md5Value = requestParam.get("md5Value");
//        String originalFilename = requestParam.get("originalFilename");
//        //校验切片是否己经上传完毕
//        boolean flag = this.checkBeforeMerge(md5Value);
//        if (!flag) {
//            return "切片未完全上传";
//        }
//        //检查是否已经有相同md5值的文件上传；主要是对名字不同，而实际文件相同的文件，直接返回成功
//        Object file_location = redisTemplate.opsForHash().get(md5Value, "file_location");
//        if (file_location != null) {
//            return "success";
//        }
//
//        //这里要特别注意，合并分片的时候一定要按照分片的索引顺序进行合并，否则文件无法使用；
//        int file_chunks = Integer.parseInt(redisTemplate.opsForHash().get(md5Value, "file_chunks").toString());
//
//        // 使用临时文件来合并分片
//        File tempFile = File.createTempFile("merge", ".tmp");
//        OutputStream outputStream = new FileOutputStream(tempFile);
//        InputStream inputStream = null;
//
//        try {
//            for (int i = 0; i < file_chunks; i++) {
//                String chunkPath = redisTemplate.opsForHash().get(md5Value, "chunk_location_" + i).toString();
//                // 从MinIO获取分片文件的输入流
//                inputStream = minIoService.getObjectStream(chunkPath);
//
//                byte[] bytes = new byte[1024 * 1024];
//                int len;
//                while ((len = inputStream.read(bytes)) != -1) {
//                    outputStream.write(bytes, 0, len);
//                }
//
//                inputStream.close();
//            }
//        } finally {
//            outputStream.close();
//        }
//
//        // 将合并后的临时文件上传到MinIO
//        String fileDir = "merged/" + md5Value.substring(0, 2) + "/" + md5Value.substring(2, 4) + "/";
//        String filePath = fileDir + originalFilename;
//
//        // 使用MinIO客户端直接上传文件
//        try (FileInputStream tempInputStream = new FileInputStream(tempFile)) {
//            filePath = minIoService.uploadFileStream(tempInputStream, filePath, "application/octet-stream");
//        }
//
//        // 更新Redis中的文件位置信息
//        redisTemplate.opsForHash().put(md5Value, "file_location", filePath);
//
//        // 删除临时文件
//        tempFile.delete();
//
//        // 删除MinIO中的分片文件
//        this.delTmpFile(md5Value);
//
//        return "success";
//    }
//
//    @GetMapping("/download")
//    public String download(String fileName, HttpServletResponse response) throws IOException {
//        response.setContentType("application/octet-stream");
//        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
//
//        // 从Redis中获取文件的存储路径
//        String md5Value = null;
//        // 这里假设fileName是原始文件名，需要根据实际情况调整
//        // 如果fileName是文件的MD5值，可以直接使用
//        // md5Value = fileName;
//
//        // 如果fileName是原始文件名，需要通过其他方式获取MD5值
//        // 这里简化处理，直接使用原始文件名作为MinIO中的对象名
//        String objectName = "merged/" + fileName;
//
//        // 从MinIO下载文件
//        minIoService.downloadFile(objectName, response);
//
//        return "success";
//    }
//
//
//    private void delTmpFile(String md5Value) throws JsonProcessingException {
//        Map map = redisTemplate.opsForHash().entries(md5Value);
//        List<String> list = new ArrayList<>();
//        List<String> chunkPaths = new ArrayList<>();
//
//        for (Object hashKey : map.keySet()) {
//            if (hashKey.toString().startsWith("chunk_location")) {
//                String chunkPath = map.get(hashKey).toString();
//                chunkPaths.add(chunkPath);
//                list.add(hashKey.toString());
//            }
//            if (hashKey.toString().startsWith("chunk_start_end_")) {
//                list.add(hashKey.toString());
//            }
//            if (hashKey.toString().startsWith("chunk_md5_")) {
//                list.add(hashKey.toString());
//            }
//        }
//
//        // 批量删除MinIO中的分片文件
//        if (!chunkPaths.isEmpty()) {
//            boolean flag = minIoService.batchDeleteFiles(chunkPaths);
//            log.info("批量删除分片文件结果: " + flag);
//        }
//
//        list.add("file_chunks");
//        list.add("file_size");
//        redisTemplate.opsForHash().delete(md5Value, list.toArray());
//    }
//
//    private Map<String, String> doRequestParam(HttpServletRequest request) {
//        Map<String, String> requestParam = new HashMap<>();
//        Enumeration<String> parameterNames = request.getParameterNames();
//        while (parameterNames.hasMoreElements()) {
//            String paramName = parameterNames.nextElement();
//            String paramValue = request.getParameter(paramName);
//            requestParam.put(paramName, paramValue);
//            log.info(paramName + "：" + paramValue);
//        }
//        log.info("----------------------------");
//        return requestParam;
//    }
//
//    /**
//     * 合并分片前检验文件整体的所有分片是否全部上传
//     *
//     * @param key
//     * @return
//     */
//    private boolean checkBeforeMerge(String key) {
//        Map map = redisTemplate.opsForHash().entries(key);
//        Object file_chunks = map.get("file_chunks");
//        int i = 0;
//        for (Object hashKey : map.keySet()) {
//            if (hashKey.toString().startsWith("chunk_md5_")) {
//                ++i;
//            }
//        }
//        if (Integer.valueOf(file_chunks.toString())==(i)) {
//            return true;
//        }
//        return false;
//    }
//}