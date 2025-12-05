package admin.service;

import org.springframework.stereotype.Service;

@Service
public interface UpdateUserInfoService {
    void updateUserInfo(Long userId);
}
