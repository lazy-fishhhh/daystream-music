package com.future.music.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InitUploadResponse {

    /**
     * 本次上传 ID
     */
    private String uploadId;

    /**
     * 最终文件对象名
     */
    private String objectName;
}