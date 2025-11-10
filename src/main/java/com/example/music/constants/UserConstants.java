package com.example.music.constants;

/**
 * 用户角色、权限相关常量
 */
public class UserConstants {
    // 角色类型
    public static final String ROLE_VISITOR = "VISITOR"; // 游客
    public static final String ROLE_COMMON = "COMMON"; // 普通用户
    public static final String ROLE_VIP = "VIP"; // VIP用户
    public static final String ROLE_MUSICIAN = "MUSICIAN"; // 音乐人
    public static final String ROLE_ADMIN = "ADMIN"; // 管理员

    // 权限标识
    public static final String PERMISSION_LOSSLESS_AUDIO = "lossless_audio"; // 无损音质权限
    public static final String PERMISSION_VIP_SKIN = "vip_skin"; // VIP皮肤权限
    public static final String PERMISSION_PAID_DISCOUNT = "paid_discount"; // 付费折扣权限
    public static final String PERMISSION_MUSIC_UPLOAD = "music_upload"; // 音乐上传权限
    public static final String PERMISSION_USER_MANAGE = "user_manage"; // 用户管理权限

    // 用户状态
    public static final int USER_STATUS_NORMAL = 0; // 正常
    public static final int USER_STATUS_BANNED = 1; // 封禁
}