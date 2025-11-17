package service;

public interface IVipCoreService {
    void checkAndUpdateVipStatus(Long userId);

    void processAutoRenewal();
}
