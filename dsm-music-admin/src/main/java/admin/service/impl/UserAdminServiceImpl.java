package admin.service.impl;

import admin.domain.dto.UserAdminDTO;
import admin.domain.vo.AdminUserVO;
import admin.mapper.AdminUserMapper;
import admin.service.UserAdminService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.mapper.UserMapper;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAdminServiceImpl implements UserAdminService {
    @Autowired
    private AdminUserMapper adminUserMapper;
    @Override
    public Result<PageResp<AdminUserVO>> Userlist(UserAdminDTO userAdminDTO) {
        //构建分页
        Page<AdminUserVO> page = new Page<>(userAdminDTO.getPageNum(), userAdminDTO.getPageSize());
        //查询
        IPage<AdminUserVO> userList = adminUserMapper.getUserList(page, userAdminDTO.getStatus(), userAdminDTO.getKey());
        //封装返回
        PageResp<AdminUserVO> pageResp = new PageResp<>();
        pageResp.setRecords(userList.getRecords());
        pageResp.setTotal(userList.getTotal());
        pageResp.setPages(userList.getPages());
        pageResp.setCurrent(userList.getCurrent());
        pageResp.setSize(userList.getSize());
        return Result.ok(pageResp);
    }
}
