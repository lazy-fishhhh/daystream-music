package com.example.music.constants;

/**
 * 业务状态流转常量（投诉、审核等）
 */
public class StatusConstants {
    // 投诉状态
    public static final String COMPLAINT_STATUS_PENDING = "PENDING"; // 待审核
    public static final String COMPLAINT_STATUS_ACCEPTED = "ACCEPTED"; // 审核通过
    public static final String COMPLAINT_STATUS_REJECTED = "REJECTED"; // 审核驳回
    public static final String COMPLAINT_STATUS_PROCESSED = "PROCESSED"; // 已处理(下架)

    // 内容审核状态
    public static final String CONTENT_STATUS_PENDING = "PENDING"; // 待审核
    public static final String CONTENT_STATUS_PASSED = "PASSED"; // 审核通过
    public static final String CONTENT_STATUS_REJECTED = "REJECTED"; // 审核驳回

    // 歌单权限状态
    public static final String PLAYLIST_STATUS_PUBLIC = "PUBLIC"; // 公开
    public static final String PLAYLIST_STATUS_PRIVATE = "PRIVATE"; // 私有
}