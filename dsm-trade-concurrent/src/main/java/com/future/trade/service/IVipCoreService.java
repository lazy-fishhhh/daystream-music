package com.future.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.future.trade.entity.vo.VipStatusVO;
import entity.pojo.util.Result;

public interface IVipCoreService {
    VipStatusVO checkAndUpdateVipStatus() throws JsonProcessingException;
    void processAutoRenewal();

    Result<String> buyVip(String packageName);
}
