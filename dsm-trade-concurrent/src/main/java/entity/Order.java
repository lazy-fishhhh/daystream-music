package entity;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体类
 * 对应表: order
 */
@Entity
@Table(name = "`order`") // order是SQL关键字，需要用反引号转义
@Data
@EqualsAndHashCode(callSuper = false)
public class Order {
    
    /**
     * 订单ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单号（唯一标识，如：VIP20240520123456）
     */
    @Column(name = "order_no", nullable = false, length = 50, unique = true)
    private String orderNo;

    /**
     * 用户ID（关联用户表id）
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 总金额（单位：分）
     */
    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    /**
     * 实付金额（扣除优惠后的金额，单位：分）
     */
    @Column(name = "pay_amount", nullable = false)
    private Integer payAmount;

    /**
     * 订单类型（1-VIP订阅，2-单曲购买，3-打赏）
     */
    @Column(name = "order_type", nullable = false)
    private Integer orderType;

    /**
     * 订单状态（0-待支付，1-已支付，2-已取消，3-已退款）
     */
    @Column(nullable = false)
    private Integer status;

    /**
     * 支付时间（支付成功后更新）
     */
    @Column(name = "pay_time")
    private LocalDateTime payTime;

    /**
     * 订单创建时间
     */
    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    /**
     * 订单更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    // 关联关系
    /**
     * 订单详情列表（一对多关系）
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    // 枚举定义
    /**
     * 订单类型枚举
     */
    public enum OrderType {
        VIP_SUBSCRIPTION(1, "VIP订阅"),
        SINGLE_PURCHASE(2, "单曲购买"),
        REWARD(3, "打赏");

        private final int code;
        private final String description;

        OrderType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static OrderType fromCode(int code) {
            for (OrderType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid order type code: " + code);
        }
    }

    /**
     * 订单状态枚举
     */
    public enum OrderStatus {
        PENDING_PAYMENT(0, "待支付"),
        PAID(1, "已支付"),
        CANCELLED(2, "已取消"),
        REFUNDED(3, "已退款");

        private final int code;
        private final String description;

        OrderStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static OrderStatus fromCode(int code) {
            for (OrderStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid order status code: " + code);
        }
    }

    // 业务方法
    
    /**
     * 检查订单是否为VIP订阅类型
     */
    @Transient
    public boolean isVipOrder() {
        return OrderType.VIP_SUBSCRIPTION.getCode() == this.orderType;
    }

    /**
     * 检查订单是否已支付
     */
    @Transient
    public boolean isPaid() {
        return OrderStatus.PAID.getCode() == this.status;
    }

    /**
     * 检查订单是否可以取消（只有待支付状态的订单可以取消）
     */
    @Transient
    public boolean canCancel() {
        return OrderStatus.PENDING_PAYMENT.getCode() == this.status;
    }

    /**
     * 获取优惠金额（总金额 - 实付金额）
     */
    @Transient
    public Integer getDiscountAmount() {
        return totalAmount - payAmount;
    }

    /**
     * 获取订单状态描述
     */
    @Transient
    public String getStatusDescription() {
        return OrderStatus.fromCode(this.status).getDescription();
    }

    /**
     * 获取订单类型描述
     */
    @Transient
    public String getOrderTypeDescription() {
        return OrderType.fromCode(this.orderType).getDescription();
    }

    /**
     * 验证订单数据有效性
     */
    public boolean isValid() {
        return orderNo != null && !orderNo.trim().isEmpty()
                && userId != null && userId > 0
                && totalAmount != null && totalAmount >= 0
                && payAmount != null && payAmount >= 0
                && payAmount <= totalAmount
                && orderType != null && orderType >= 1 && orderType <= 3
                && status != null && status >= 0 && status <= 3;
    }

    /**
     * 标记订单为已支付
     */
    public void markAsPaid(LocalDateTime payTime) {
        this.status = OrderStatus.PAID.getCode();
        this.payTime = payTime;
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 标记订单为已取消
     */
    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED.getCode();
        this.updatedTime = LocalDateTime.now();
    }
}