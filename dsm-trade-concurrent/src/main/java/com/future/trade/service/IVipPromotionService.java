package com.future.trade.service;

import entity.pojo.util.Result;

public interface IVipPromotionService {
    Result<Void> triggerFreeTrial(Long userId);

    Result<Void> executePromotionActivity(Long activityId, Long userId);

//    Result<RenewalIncentive> getRenewalIncentive(Long userId);

//    void distributeVipCoupons(Long userId, CouponType couponType);
}
