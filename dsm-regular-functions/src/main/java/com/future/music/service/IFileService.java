package com.future.music.service;

import org.springframework.web.multipart.MultipartFile;

import entity.dto.FileUploadDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 文件服务接口
 * 定义分片上传、合并、下载等文件处理相关方法
 *
 * @author future
 * @date 2023-09-01
 */
public interface IFileService {

    /**
     * 检验分片文件是否已经上传到MinIO
     *
     * @param fileMd5  整体文件md5值
     * @param chunk    当前上传分片在所有分片文件中索引位置
     * @param chunkMd5 分片文件的md5值
     * @return 是否已经上传
     */
    boolean check(String fileMd5, String chunk, String chunkMd5);

    /**
     * 分片上传接口
     *
     * @param request       HttpServletRequest
     * @param multipartFile 分片文件
     * @return 上传结果信息
     * @throws IOException IO异常
     */
    String upload(HttpServletRequest request, MultipartFile multipartFile) throws IOException;

    /**
     * 合并分片文件接口
     *
     * @param request HttpServletRequest
     * @return 合并结果信息
     * @throws IOException IO异常
     */
    String merge(HttpServletRequest request) throws IOException;

    /**
     * 文件下载接口
     *
     * @param fileName 文件名
     * @param response HttpServletResponse
     * @throws IOException IO异常
     */
    void download(String fileName, HttpServletResponse response) throws IOException;

    /**
     * 大文件上传（封装了检查、上传分片、合并的完整流程）
     * @param fileUploadDTO 文件上传DTO
     * @return 上传结果
     */
    String uploadLargeFile(FileUploadDTO fileUploadDTO);
}