package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface UserInfoService {
    Result getUserInfo();
    Result getOtherUserInfo(Long userId);
}
