package com.future.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.future.trade.entity.vo.VipStatusVO;

public interface IVipCoreService {
    VipStatusVO checkAndUpdateVipStatus() throws JsonProcessingException;
    void processAutoRenewal();
}
