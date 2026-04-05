package com.future.player.entity.vo;

import lombok.Data;



@Data
public class MusicianDetailVO {
    /**
     * 音乐人id
     */
    private String musicianId;
    /**
     * 艺名
     */
    private String stageName;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 描述
     */
    private String introduction;
    /**
     * 粉丝数
     */
    private long fansCount;
    /**
     * 关联用户id
     */
    private Long userId;
    /**
     * 是否关注
     */
    private boolean isFollow;
}
