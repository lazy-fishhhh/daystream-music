package com.example.music.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

public interface IMinIoService {

    /**
     * 上传文件到MinIO
     * @param file 上传的文件
     * @return 存储路径
     */
    String uploadFile(MultipartFile file);

    /**
     * 从MinIO下载文件
     * @param objectName 文件名
     * @param response 响应流（用于浏览器下载）
     */
    void downloadFile(String objectName, HttpServletResponse response);

    /**
     * 删除MinIO中的文件
     * @param objectName 文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String objectName);

    /**
     * 获取文件输入流
     * @param objectName 文件名
     * @return 输入流
     */
    InputStream getFileInputStream(String objectName);

    //判断文件是否存在
    boolean isObjectExists(String objectName);

    Instant getObjectLastModified(String bucketName, String objectName) throws Exception;

    boolean isAllowedFile(MultipartFile file, String type);

}
