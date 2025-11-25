package com.future.trade.service;

import entity.pojo.util.Result;
import io.swagger.models.auth.In;
import org.springframework.transaction.annotation.Transactional;

public interface IBasicOrderService {

    @Transactional(rollbackFor = Exception.class)
    Result<String> rewardBasicOrder(Long receiverId, Integer amount);


}
