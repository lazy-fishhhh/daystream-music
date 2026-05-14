package com.future.music.chunk;

import lombok.Data;

@Data
public class CompleteUploadRequest {

    /**
     * 上传 ID
     */
    private String uploadId;

    /**
     * 最终文件对象名
     */
    private String objectName;

    /**
     * 分片总数
     */
    private Integer totalChunks;
}