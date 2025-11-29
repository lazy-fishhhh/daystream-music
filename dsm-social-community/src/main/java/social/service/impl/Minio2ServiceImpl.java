package social.service.impl;

import cn.hutool.core.lang.UUID;
import entity.pojo.util.Result;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import social.service.Minio2Service;
@Service
@Slf4j
public class Minio2ServiceImpl implements Minio2Service {
    @Resource
    private MinioClient minioClient;
    @Value("${minio.bucketName}")
    private String bucketName;

    //上传照片
    @Override
    public Result uploadFile(MultipartFile file) {
        //判断文件是否为空
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        //判断桶是否存在，不存在则创建
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            //生成文件名
            String originalFilename = file.getOriginalFilename();
            String exceptionName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "image/" + UUID.randomUUID().toString().replace("-", "") + exceptionName;
            //上传文件
            minioClient.putObject(
                    PutObjectArgs.builder().
                            bucket(bucketName).
                            object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType()).build());
            //返回文件url
            return Result.ok(minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .method(Method.GET)
                            .build()
            ));
        } catch (Exception e) {
            log.error("文件上传到 MinIO 失败: {}", e.getMessage(), e);
            return Result.fail("文件上传失败");
        }
    }
}
