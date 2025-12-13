package com.future.music.entity.message;

import lombok.Data;
import java.io.Serializable;

/**
 * 音乐下载量更新消息
 * 需实现Serializable接口，保证消息可序列化
 */
@Data
public class MusicDownloadCountMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    // 音乐ID
    private Long musicId;
}