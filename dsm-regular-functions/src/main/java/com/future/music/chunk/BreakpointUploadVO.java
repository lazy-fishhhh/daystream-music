package com.future.music.chunk;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 断点续传返回：已上传分片列表
 */
@Data
public class BreakpointUploadVO {
    // 是否已经完整上传（秒传）
    private boolean exist;

    // 上传ID（exist=false 时返回）
    private String uploadId;

    // 原始文件对象名（exist=false 时返回）
    private String objectName;

    // 已上传的分片序号
    private Set<Integer> uploadedChunks = new HashSet<>();

    // 文件地址（exist=true 时返回）
    private String fileUrl;
}