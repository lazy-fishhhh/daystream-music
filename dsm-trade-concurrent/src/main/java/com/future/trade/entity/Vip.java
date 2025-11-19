package com.future.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "vip")
@Data
public class Vip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "package_id", nullable = false)
    private Long packageId;
    
    @Column(nullable = false)
    private Integer level;
    
    // 时间相关字段
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "renewal_time")
    private LocalDateTime renewalTime;
    
    // 状态字段
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VipStatus status = VipStatus.ACTIVE;

    // 自动续费
    @Column(name = "auto_renew")
    private Boolean autoRenew = false;
    
    @Column(name = "renewal_count")
    private Integer renewalCount = 0;
    
    // 订单关联
    @Column(name = "order_id")
    private Long orderId;
    
    @Enumerated(EnumType.STRING)
    private VipSource source = VipSource.PURCHASE;
    
    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
    
    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
    
    // 业务方法
    public boolean isActive() {
        return status == VipStatus.ACTIVE && endTime.isAfter(LocalDateTime.now());
    }
    
    public long getRemainingDays() {
        return Duration.between(LocalDateTime.now(), endTime).toDays();
    }
    
    public enum VipStatus {
        ACTIVE, EXPIRED, CANCELLED
    }
    
    public enum VipSource {
        PURCHASE, GIFT, PROMOTION
    }
}