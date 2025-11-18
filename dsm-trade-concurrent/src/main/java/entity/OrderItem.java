package entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 订单详情实体类
 * 对应表: order_item
 */
@Entity
@Table(name = "order_item")
@Data
@EqualsAndHashCode(callSuper = false)
public class OrderItem {
    
    /**
     * 订单详情ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单ID（关联主订单表order的id）
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 商品类型（1-VIP套餐，2-单曲，3-打赏）
     */
    @Column(name = "product_type", nullable = false)
    private Integer productType;

    /**
     * 商品ID（product_type=1时，关联vip_package表的id）
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 商品名称（冗余存储避免关联查询）
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /**
     * 商品单价（单位：分）
     */
    @Column(nullable = false)
    private Integer price;

    /**
     * 购买数量
     */
    @Column(nullable = false)
    private Integer quantity = 1;

    // 关联关系
    /**
     * 所属订单（多对一关系）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    // 枚举定义
    /**
     * 商品类型枚举
     */
    public enum ProductType {
        VIP_PACKAGE(1, "VIP套餐"),
        SINGLE_SONG(2, "单曲"),
        REWARD(3, "打赏");

        private final int code;
        private final String description;

        ProductType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static ProductType fromCode(int code) {
            for (ProductType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid product type code: " + code);
        }
    }

    // 业务方法
    
    /**
     * 检查是否为VIP套餐商品
     */
    @Transient
    public boolean isVipPackage() {
        return ProductType.VIP_PACKAGE.getCode() == this.productType;
    }

    /**
     * 计算商品总价（单价 × 数量）
     */
    @Transient
    public Integer getTotalPrice() {
        return price * quantity;
    }

    /**
     * 获取商品类型描述
     */
    @Transient
    public String getProductTypeDescription() {
        return ProductType.fromCode(this.productType).getDescription();
    }

    /**
     * 验证订单详情数据有效性
     */
    public boolean isValid() {
        return orderId != null && orderId > 0
                && productType != null && productType >= 1 && productType <= 3
                && productId != null && productId > 0
                && productName != null && !productName.trim().isEmpty()
                && price != null && price >= 0
                && quantity != null && quantity > 0;
    }

    /**
     * 创建VIP套餐订单详情
     */
    public static OrderItem createVipPackageItem(Long orderId, VipPackage vipPackage) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductType(ProductType.VIP_PACKAGE.getCode());
        item.setProductId(vipPackage.getId());
        item.setProductName(vipPackage.getPackageName());
        item.setPrice(vipPackage.getCurrentPrice());
        item.setQuantity(1);
        return item;
    }
}