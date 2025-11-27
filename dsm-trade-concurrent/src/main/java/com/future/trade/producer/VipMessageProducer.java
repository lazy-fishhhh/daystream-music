package com.future.trade.producer;

import com.future.trade.entity.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * VIP消息生产者服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipMessageProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 发送VIP支付成功消息
     */
    public void sendPaymentSuccessMessage(String orderNo, Long userId, Long vipId, Long packageId, Integer amount) {
        try {
            VipPaymentSuccessMessage message = new VipPaymentSuccessMessage(orderNo, userId, vipId, packageId, amount);
            rabbitTemplate.convertAndSend("vip.direct", "vip.payment.success", message);
            log.info("发送VIP支付成功消息: orderNo={}, userId={}", orderNo, userId);
        } catch (Exception e) {
            log.error("发送VIP支付成功消息失败: orderNo={}, userId={}", orderNo, userId, e);
        }
    }
    
    /**
     * 发送VIP自动续费消息
     */
    public void sendAutoRenewMessage(Long userId, Long vipId, Long packageId) {
        try {
            VipAutoRenewMessage message = new VipAutoRenewMessage(userId, vipId, packageId);
            rabbitTemplate.convertAndSend("vip.direct", "vip.auto.renew", message);
            log.info("发送VIP自动续费消息: userId={}, vipId={}", userId, vipId);
        } catch (Exception e) {
            log.error("发送VIP自动续费消息失败: userId={}, vipId={}", userId, vipId, e);
        }
    }
    
    /**
     * 发送VIP状态变更消息
     */
    public void sendStatusChangeMessage(Long userId, Integer oldStatus, Integer newStatus, String reason) {
        try {
            VipStatusChangeMessage message = new VipStatusChangeMessage(userId, oldStatus, newStatus, reason);
            rabbitTemplate.convertAndSend("vip.direct", "vip.status.change", message);
            log.info("发送VIP状态变更消息: userId={}, {} -> {}", userId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("发送VIP状态变更消息失败: userId={}", userId, e);
        }
    }
    
    /**
     * 发送VIP退款消息
     */
    public void sendRefundMessage(String orderNo, Long userId, Integer refundAmount, String refundReason) {
        try {
            VipRefundMessage message = new VipRefundMessage(orderNo, userId, refundAmount, refundReason);
            rabbitTemplate.convertAndSend("vip.direct", "vip.refund", message);
            log.info("发送VIP退款消息: orderNo={}, userId={}, amount={}", orderNo, userId, refundAmount);
        } catch (Exception e) {
            log.error("发送VIP退款消息失败: orderNo={}, userId={}", orderNo, userId, e);
        }
    }
    
    /**
     * 发送VIP秒杀结果消息
     */
    public void sendSeckillResultMessage(Long userId, Long activityId, Boolean success, String message, String token) {
        try {
            VipSeckillResultMessage resultMessage = new VipSeckillResultMessage(userId, activityId, success, message, token);
            rabbitTemplate.convertAndSend("vip.direct", "vip.seckill.result", resultMessage);
            log.info("发送VIP秒杀结果消息: userId={}, activityId={}, success={}", userId, activityId, success);
        } catch (Exception e) {
            log.error("发送VIP秒杀结果消息失败: userId={}, activityId={}", userId, activityId, e);
        }
    }
    
    /**
     * 发送订单超时消息（使用死信队列实现延迟）
     */
    public void sendOrderTimeoutMessage(String orderNo, Long userId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("orderNo", orderNo);
            message.put("userId", userId);
            message.put("createTime", System.currentTimeMillis());
            
            // 发送到延迟队列（通过TTL+死信队列实现）
            rabbitTemplate.convertAndSend("vip.direct", "vip.order.timeout", message);
            log.info("发送订单超时消息: orderNo={}, userId={}", orderNo, userId);
        } catch (Exception e) {
            log.error("发送订单超时消息失败: orderNo={}, userId={}", orderNo, userId, e);
        }
    }
}