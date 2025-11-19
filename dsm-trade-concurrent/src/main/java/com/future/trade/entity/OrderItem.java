package com.future.trade.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 订单详情表实体类
 */
@Data
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId; // 关联主订单ID

    @Column(name = "product_type", nullable = false)
    @Convert(converter = ProductTypeConverter.class)
    private int productType; // 商品类型（1-VIP套餐，2-单曲，3-打赏）

    @Column(name = "product_id", nullable = false)
    private Long productId; // 商品ID（关联对应商品表）

    @Column(name = "product_name", nullable = false)
    private String productName; // 商品名称

    @Column(name = "price", nullable = false)
    private Integer price; // 商品单价（单位：音浪）

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1; // 购买数量（默认1）


    /**
     * 商品类型枚举
     */
    public enum ProductType {
        VIP_PACKAGE(1, "VIP套餐"),
        SONG(2, "单曲"),
        REWARD(3, "打赏"),
        GOODS(4, "商品");

        private final int code;
        private final String desc;

        ProductType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        // 根据code获取枚举
        public static ProductType getByCode(int code) {
            for (ProductType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 商品类型转换器（JPA枚举与数据库字段映射）
     */
    @Converter
    public static class ProductTypeConverter implements AttributeConverter<ProductType, Integer> {
        @Override
        public Integer convertToDatabaseColumn(ProductType attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public ProductType convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : ProductType.getByCode(dbData);
        }
    }
}