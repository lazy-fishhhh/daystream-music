package admin.controller;

import admin.domain.dto.UserAdminDTO;
import admin.domain.vo.AdminUserVO;
import admin.service.UserAdminService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/adminUser")
@Api(tags = "用户管理")
public class AdminUserController {
    @Autowired
    private UserAdminService userAdminService;
    @PostMapping("/list")
    @ApiOperation(value = "用户列表")
    public Result<PageResp<AdminUserVO>> Userlist(@Valid @RequestBody UserAdminDTO userAdminDTO)
    {
        return userAdminService.Userlist(userAdminDTO);

    }
}
