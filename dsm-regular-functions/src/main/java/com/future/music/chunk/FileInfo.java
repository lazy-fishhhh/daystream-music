package com.future.music.chunk;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("file_info")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件MD5哈希值（秒传核心）
     */
    private String fileMd5;

    /**
     * MinIO对象名称
     */
    private String objectName;

    /**
     * 上传状态：0-未完成，1-已完成
     */
    private Integer uploadStatus;
}