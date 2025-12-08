package com.future.music.service.impl;

import com.future.music.service.IMinIoService;
import constants.SystemConstants;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
class MinIoServiceImpl implements IMinIoService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.proxy-host}")
    private String proxyHost;

    /**
     * 上传文件
     */
    @Override
    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // 检查桶是否存在，不存在则创建
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 返回文件存储路径（格式：桶名/文件名）
            return  proxyHost+ "/"+ bucketName + "/" + fileName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 下载文件
     */
    @Override
    public void downloadFile(String objectName, HttpServletResponse response) {
        String s=objectName.substring(objectName.lastIndexOf("/") + 1);

        try (InputStream in = getFileInputStream(objectName);
             OutputStream out = response.getOutputStream()) {

            // 设置响应头（支持浏览器下载）
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(s, StandardCharsets.UTF_8.name()));

            // 写入响应流
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @Override
    public boolean deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName.substring(objectName.lastIndexOf("/") + 1))
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 批量删除桶文件
    @Override
    public boolean batchDeleteFiles(List<String> objectNames) {
        try {
            for (String objectName : objectNames) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName.substring(objectName.lastIndexOf("/") + 1))
                                .build()
                );
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件输入流
     */
    @Override
    public InputStream getFileInputStream(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName.substring(objectName.lastIndexOf("/") + 1))
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取文件流失败：" + e.getMessage());
        }
    }

    //判断文件是否存在
    @Override
    public boolean isObjectExists(String objectName) {
        try {
            // 调用statObject方法获取文件信息，如果文件不存在会抛出异常
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName.substring(objectName.lastIndexOf("/") + 1))
                            .build()
            );
            // 没有抛出异常，说明文件存在
            return true;
        } catch (ErrorResponseException e) {
            // 捕获404异常（文件不存在）
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            // 其他错误（如桶不存在等）
            throw new RuntimeException("判断文件存在性失败：" + e.getMessage());
        } catch (InvalidKeyException | InvalidResponseException | IOException
                 | NoSuchAlgorithmException | ServerException | InsufficientDataException
                 | InternalException | XmlParserException e) {
            // 处理其他异常
            throw new RuntimeException("判断文件存在性失败：" + e.getMessage());
        }
    }

    //获取文件最后修改时间的方法
    @Override
    public Instant getObjectLastModified(String objectName) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        return stat.lastModified().toInstant(); // 返回文件最后修改时间
    }

    //判断是否是正确的格式
    @Override
    public boolean isAllowedFile(MultipartFile file, String type) {
        List<String> allowedExtensions = new ArrayList<>();
        if (type.equals("music")) { // 新增音乐类型
            // 常用音乐格式：MP3（主流）、FLAC（无损）、AAC（高清）、WAV（无损）
            allowedExtensions = Arrays.asList("mp3", "flac", "aac", "wav");
        }else if (type.equals("picture")) {
            allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");
        }else if (type.equals("lyric")) {
            allowedExtensions = Arrays.asList("lrc","txt");
        }

        if (file.isEmpty()) {
            return false;
        }

        // 获取文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return false;
        }

        // 获取文件扩展名
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        return allowedExtensions.contains(extension);
    }

    /**
     * 生成MinIO文件预签名URL（临时可访问）
     * @param objectName 文件路径（如 "audio/2025/xxx.mp3"）
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL（前端可直接用于音频播放）
     */
    @Override
    public String generatePresignedUrl(String objectName, int expireSeconds) {
        try {
            // MinIO客户端生成预签名URL
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName) // 你的MinIO桶名
                            .object(objectName.substring(objectName.lastIndexOf("/") + 1))
                            .expiry(expireSeconds)
                            .build()
            );
            return url;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从MinIO下载图片为BufferedImage
     */
    @Override
    public BufferedImage downloadImage(String objectName) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName.substring(objectName.lastIndexOf("/") + 1))
                        .build()
        )) {
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 上传图片输入流到MinIO
     */
    @Override
    public String uploadFileStream(InputStream is, String fileName, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(is, is.available(), -1)
                            .contentType(contentType)
                            .build()
            );
            return proxyHost+ "/"+ bucketName + "/" + fileName; // 返回存储路径，后续可通过预签名URL访问
        } catch (Exception e) {
            return null;
        }
    }

}
