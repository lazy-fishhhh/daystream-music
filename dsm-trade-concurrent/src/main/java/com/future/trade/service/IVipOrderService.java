package com.future.trade.service;

import com.future.trade.entity.Order;
import entity.pojo.util.Result;

import javax.transaction.Transactional;
import java.util.List;

public interface IVipOrderService {

    @Transactional
    Result<String> buyVip(String packageName);

    Order createVipOrder(Long packageId);

    Result<List<Order>> getUserOrderHistory(Long userId);

    Result<Void> processOrderRefund(Long orderId);


}
