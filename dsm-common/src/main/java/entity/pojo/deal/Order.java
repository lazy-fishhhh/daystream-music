package entity.pojo.deal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;                 // 主键ID
    private String orderNo;          // 订单号
    private Long userId;             // 用户ID
    private BigDecimal totalAmount;  // 总金额
    private BigDecimal payAmount;    // 实付金额
    private BigDecimal discountAmount; // 折扣金额
    private Integer pointDeduct;     // 抵扣积分
    private Integer orderType;       // 类型（1-VIP订阅，2-单曲购买，3-打赏）
    private Integer status;          // 状态（0-待支付，1-已支付，2-已取消，3-已退款）
    private LocalDateTime payTime;   // 支付时间
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
}