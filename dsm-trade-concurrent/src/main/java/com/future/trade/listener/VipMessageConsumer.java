package com.future.trade.listener;

import com.future.trade.producer.VipMessageProducer;
import com.future.trade.entity.Vip;
import com.future.trade.entity.message.VipAutoRenewMessage;
import com.future.trade.entity.message.VipPaymentSuccessMessage;
import com.future.trade.entity.message.VipRefundMessage;
import com.future.trade.entity.message.VipSeckillResultMessage;
import com.future.trade.entity.message.VipStatusChangeMessage;
import com.future.trade.mapper.VipMapper;
import com.future.trade.service.IVipCoreService;
import com.future.trade.service.IVipOrderService;
import com.future.trade.service.IVipPromotionService;
import com.future.trade.constants.RabbitConstants; // 引入常量类
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.future.trade.constants.VipRedisConstant.USER_VIP_INFO_PREFIX;

/**
 * VIP消息消费者服务 - 完全基于延迟插件（无死信队列）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipMessageConsumer {

    private final IVipCoreService vipCoreService;
    private final IVipOrderService vipOrderService;
    private final IVipPromotionService vipPromotionService;
    private final VipMapper vipMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final VipMessageProducer vipMessageProducer;

    // ==================== 普通业务消息处理 ====================

    /**
     * 处理VIP支付成功消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.VIP_PAYMENT_SUCCESS_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitConstants.VIP_DIRECT_EXCHANGE, type = "direct"),
            key = RabbitConstants.ROUTING_KEY_PAYMENT_SUCCESS
    ))
    public void handlePaymentSuccess(VipPaymentSuccessMessage message) {
        log.info("接收到VIP支付成功消息: {}", message);
        try {
            Vip vip = vipMapper.selectById(message.getVipId());
            if (vip != null) {
                vip.setStatus(Vip.VipStatus.ACTIVE);
                vip.setUpdatedTime(LocalDateTime.now());
                vipMapper.updateById(vip);
                stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + message.getUserId());
                vipMessageProducer.sendStatusChangeMessage(
                        message.getUserId(),
                        Vip.VipStatus.EXPIRED.ordinal(),
                        Vip.VipStatus.ACTIVE.ordinal(),
                        "支付成功开通VIP"
                );
                log.info("VIP支付成功处理完成: userId={}, vipId={}", message.getUserId(), message.getVipId());
            } else {
                log.error("VIP记录不存在: vipId={}", message.getVipId());
            }
        } catch (Exception e) {
            log.error("处理VIP支付成功消息异常: {}", message, e);
        }
    }

    /**
     * 处理VIP自动续费消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.VIP_AUTO_RENEW_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitConstants.VIP_DIRECT_EXCHANGE, type = "direct"),
            key = RabbitConstants.ROUTING_KEY_AUTO_RENEW
    ))
    public void handleAutoRenew(VipAutoRenewMessage message) {
        log.info("接收到VIP自动续费消息: {}", message);
        try {
//            vipOrderService.processSingleUserRenewal(message.getUserId(), message.getVipId(), message.getPackageId());
            log.info("VIP自动续费处理完成: userId={}, vipId={}", message.getUserId(), message.getVipId());
        } catch (Exception e) {
            log.error("处理VIP自动续费消息异常: {}", message, e);
        }
    }

    /**
     * 处理VIP状态变更消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.VIP_STATUS_CHANGE_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitConstants.VIP_DIRECT_EXCHANGE, type = "direct"),
            key = RabbitConstants.ROUTING_KEY_STATUS_CHANGE
    ))
    public void handleStatusChange(VipStatusChangeMessage message) {
        log.info("接收到VIP状态变更消息: {}", message);
        try {
            stringRedisTemplate.delete(USER_VIP_INFO_PREFIX + message.getUserId());
            sendStatusChangeNotification(message);
            logStatusChange(message);
            log.info("VIP状态变更处理完成: userId={}, {}->{}",
                    message.getUserId(), message.getOldStatus(), message.getNewStatus());
        } catch (Exception e) {
            log.error("处理VIP状态变更消息异常: {}", message, e);
        }
    }

    /**
     * 处理VIP退款消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.VIP_REFUND_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitConstants.VIP_DIRECT_EXCHANGE, type = "direct"),
            key = RabbitConstants.ROUTING_KEY_REFUND
    ))
    public void handleRefund(VipRefundMessage message) {
        log.info("接收到VIP退款消息: {}", message);
        try {
//            vipOrderService.processOrderRefundByMessage(message);
            log.info("VIP退款处理完成: orderNo={}, userId={}", message.getOrderNo(), message.getUserId());
        } catch (Exception e) {
            log.error("处理VIP退款消息异常: {}", message, e);
        }
    }

    /**
     * 处理VIP秒杀结果消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.VIP_SECKILL_RESULT_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitConstants.VIP_DIRECT_EXCHANGE, type = "direct"),
            key = RabbitConstants.ROUTING_KEY_SECKILL_RESULT
    ))
    public void handleSeckillResult(VipSeckillResultMessage message) {
        log.info("接收到VIP秒杀结果消息: {}", message);
        try {
//            if (message.getSuccess()) {
//                vipPromotionService.grantFreeTrial(
//                        message.getUserId(),
//                        7,
//                        message.getToken()
//                );
//            }
            sendSeckillResultNotification(message);
            log.info("VIP秒杀结果处理完成: userId={}, success={}", message.getUserId(), message.getSuccess());
        } catch (Exception e) {
            log.error("处理VIP秒杀结果消息异常: {}", message, e);
        }
    }

    // ==================== 延迟插件：订单超时取消 ====================

    /**
     * 处理订单超时取消消息（仅延迟插件，无死信）
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitConstants.VIP_ORDER_TIMEOUT_QUEUE, durable = "true"),
            exchange = @Exchange(
                    value = RabbitConstants.VIP_DELAY_DIRECT_EXCHANGE,
                    type = "direct",
                    delayed = "true"
            ),
            key = RabbitConstants.ROUTING_KEY_ORDER_TIMEOUT
    ))
    public void handleOrderTimeout(Map<String, Object> message) {
        String orderNo = (String) message.get("orderNo");
        Long userId = ((Number) message.get("userId")).longValue();
        log.info("接收到订单超时取消消息: orderNo={}, userId={}", orderNo, userId);
        try {
//            vipOrderService.cancelExpiredOrder(orderNo);
            log.info("订单超时取消处理完成: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("处理订单超时取消消息异常: orderNo={}", orderNo, e);
        }
    }

    // ==================== 辅助方法 ====================

    private void sendStatusChangeNotification(VipStatusChangeMessage message) {
        log.info("发送状态变更通知: userId={}, reason={}", message.getUserId(), message.getReason());
    }

    private void logStatusChange(VipStatusChangeMessage message) {
        log.info("记录状态变更日志: userId={}, {}->{}, reason={}",
                message.getUserId(), message.getOldStatus(), message.getNewStatus(), message.getReason());
    }

    private void sendSeckillResultNotification(VipSeckillResultMessage message) {
        log.info("发送秒杀结果通知: userId={}, success={}, message={}",
                message.getUserId(), message.getSuccess(), message.getMessage());
    }
}