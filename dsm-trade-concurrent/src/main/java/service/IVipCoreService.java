package service;

import entity.pojo.util.Result;
import entity.vo.VipStatusVO;

public interface IVipCoreService {
    VipStatusVO checkAndUpdateVipStatus();
    void processAutoRenewal();
}
