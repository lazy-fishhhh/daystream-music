package entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VIP状态视图对象
 * 用于展示用户的VIP相关信息
 */
@Data
public class VipStatusVO {
    /**
     * 是否为VIP用户（0-否，1-是）
     */
    private Integer isVip;
    
    /**
     * VIP过期时间
     */
    private LocalDateTime vipExpireTime;
    
    /**
     * 剩余VIP天数（如果不是VIP则为0）
     */
    private Integer remainingDays;
    
    /**
     * VIP等级（1-普通VIP，2-高级VIP等）
     */
    private Integer vipLevel;
    
    /**
     * VIP权限列表，如无损音质、VIP皮肤等
     */
    private List<String> vipPermissions;
    
    /**
     * 是否已使用过免费会员（0-否，1-是）
     */
    private Integer freeVipUsed;
    
    /**
     * 上次开通VIP的套餐名称
     */
    private String lastPackageName;
}
