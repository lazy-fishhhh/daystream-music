package com.future.music.chunk;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分片上传记录（断点续传核心）
 */
@Data
@TableName("file_chunk")
public class FileChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 上传标识
    private String uploadId;

    // 分片序号
    private Integer chunkIndex;

    // 文件原始名字
    private String objectName;

    // 上传时间
    private LocalDateTime createTime;
}