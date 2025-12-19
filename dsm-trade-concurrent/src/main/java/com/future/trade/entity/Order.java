package com.future.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主订单表实体类
 */
@Data
@Entity
@TableName("`order`") // 表名包含关键字，需用反引号
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo; // 订单号（唯一）

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount; // 总金额（单位：音浪）

    @Column(name = "pay_amount", nullable = false)
    private Integer payAmount; // 实付金额（单位：音浪）

    @Column(name = "order_type", nullable = false)
    @Convert(converter = OrderTypeConverter.class)
    private int orderType; // 订单类型（1-VIP订阅，2-单曲购买，3-打赏, 4-购买商品）

    @Column(name = "status", nullable = false)
    @Convert(converter = OrderStatusConverter.class)
    private int status; // 订单状态（0-待支付，1-已支付，2-已取消，3-已退款）

    @Column(name = "pay_time")
    private LocalDateTime payTime; // 支付时间

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime; // 创建时间

    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime; // 更新时间


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems; // 订单商品项列表



    /**
     * 订单类型枚举
     */
    public enum OrderType {
        VIP_SUBSCRIBE(1, "VIP订阅"),
        SONG_PURCHASE(2, "单曲购买"),
        REWARD(3, "打赏"),
        GOODS(4, "商品"),
        ;

        private final int code;
        private final String desc;

        OrderType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        // 根据code获取枚举
        public static OrderType getByCode(int code) {
            for (OrderType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
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
        private final String desc;

        OrderStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        // 根据code获取枚举
        public static OrderStatus getByCode(int code) {
            for (OrderStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 订单类型转换器（JPA枚举与数据库字段映射）
     */
    @Converter
    public static class OrderTypeConverter implements AttributeConverter<OrderType, Integer> {
        @Override
        public Integer convertToDatabaseColumn(OrderType attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public OrderType convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : OrderType.getByCode(dbData);
        }
    }

    /**
     * 订单状态转换器（JPA枚举与数据库字段映射）
     */
    @Converter
    public static class OrderStatusConverter implements AttributeConverter<OrderStatus, Integer> {
        @Override
        public Integer convertToDatabaseColumn(OrderStatus attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public OrderStatus convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : OrderStatus.getByCode(dbData);
        }
    }
}