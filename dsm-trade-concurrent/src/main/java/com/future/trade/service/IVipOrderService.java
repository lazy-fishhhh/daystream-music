package com.future.trade.service;

import com.future.trade.entity.vo.OrderDetailVO;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import javax.transaction.Transactional;

public interface IVipOrderService {

    @Transactional
    Result<String> buyVip(String packageName);

    Result<PageResp<OrderDetailVO>> getUserOrderHistory(PageReq pageReq);

    Result<Void> processOrderRefund(Long orderId);


}
