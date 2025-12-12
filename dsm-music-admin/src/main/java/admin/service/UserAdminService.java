package admin.service;

import admin.domain.dto.*;
import admin.domain.vo.AdminUserVO;
import admin.domain.vo.BanUserStatusVO;
import admin.domain.vo.DetailUserVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface UserAdminService {
    Result<PageResp<AdminUserVO>> Userlist(UserAdminDTO userAdminDTO);

    Result<BanUserStatusVO> BanUser(Long userId);

    Result<DetailUserVO> getDetailUser(Integer id);

    Result<String> UpdateUser(UpdateUserDTO updateUserDTO);

    Result<String> AddUser(AddUserDTO addUserDTO);

    Result<String> DeleteUser(Long userId);

    Result<String> DeleteUsersList(DeleteUserDTO deleteUserDTO);
}
