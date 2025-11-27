package com.future.trade.constants;

/**
 * RabbitMQ 相关常量定义
 */
public final class RabbitConstants {

    // ==================== 交换机 (Exchanges) ====================

    /**
     * VIP 业务主交换机（处理普通消息）
     */
    public static final String VIP_DIRECT_EXCHANGE = "vip.direct";

    /**
     * VIP 延迟交换机（处理延迟消息）
     */
    public static final String VIP_DELAY_DIRECT_EXCHANGE = "vip.delay.direct";

    // ==================== 队列 (Queues) ====================

    /**
     * VIP 支付成功队列
     */
    public static final String VIP_PAYMENT_SUCCESS_QUEUE = "vip.payment.success.queue";

    /**
     * VIP 自动续费队列
     */
    public static final String VIP_AUTO_RENEW_QUEUE = "vip.auto.renew.queue";

    /**
     * VIP 状态变更队列
     */
    public static final String VIP_STATUS_CHANGE_QUEUE = "vip.status.change.queue";

    /**
     * VIP 退款队列
     */
    public static final String VIP_REFUND_QUEUE = "vip.refund.queue";

    /**
     * VIP 秒杀结果队列
     */
    public static final String VIP_SECKILL_RESULT_QUEUE = "vip.seckill.result.queue";

    /**
     * VIP 订单超时取消队列（延迟队列）
     */
    public static final String VIP_ORDER_TIMEOUT_QUEUE = "vip.order.timeout.queue";

    // ==================== 路由键 (Routing Keys) ====================

    /**
     * 支付成功路由键
     */
    public static final String ROUTING_KEY_PAYMENT_SUCCESS = "vip.payment.success";

    /**
     * 自动续费路由键
     */
    public static final String ROUTING_KEY_AUTO_RENEW = "vip.auto.renew";

    /**
     * 状态变更路由键
     */
    public static final String ROUTING_KEY_STATUS_CHANGE = "vip.status.change";

    /**
     * 退款路由键
     */
    public static final String ROUTING_KEY_REFUND = "vip.refund";

    /**
     * 秒杀结果路由键
     */
    public static final String ROUTING_KEY_SECKILL_RESULT = "vip.seckill.result";

    /**
     * 订单超时取消路由键
     */
    public static final String ROUTING_KEY_ORDER_TIMEOUT = "vip.order.timeout";

    /**
     * 私有构造函数，防止该类被实例化
     */
    private RabbitConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}