package com.future.player.entity.vo;

import jakarta.validation.constraints.Future;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MusicianInfoVO {
    /**
     * 音乐人id
     */
    private String id;
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
    private Integer fansCount;
    /**
     * 累计收入
     */
    private BigDecimal totalIncome;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 关联用户id
     */
    private Long userId;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 音乐人标签
     */
    private List<Long> tags;
}
