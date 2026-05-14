package com.future.music.chunk;

import lombok.Data;

@Data
public class InitUploadRequest {

    /**
     * 文件MD5哈希值
     */
    private String fileMd5;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 文件总大小
     */
    private Long fileSize;

    /**
     * 分片总数
     */
    private Integer totalChunks;

    /**
     * 文件 MIME 类型
     */
    private String contentType;
}