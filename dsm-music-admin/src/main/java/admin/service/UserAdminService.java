package admin.service;

import admin.domain.dto.UserAdminDTO;
import admin.domain.vo.AdminUserVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface UserAdminService {
    Result<PageResp<AdminUserVO>> Userlist(UserAdminDTO userAdminDTO);
}
