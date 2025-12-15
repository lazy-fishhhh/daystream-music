package admin.controller;

import admin.domain.dto.*;
import admin.domain.vo.AdminUserVO;
import admin.domain.vo.BanUserStatusVO;
import admin.domain.vo.DetailUserVO;
import admin.service.UserAdminService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/adminUser")
@Api(tags = "用户管理")
@PreAuthorize("hasRole('管理员')")
public class AdminUserController {
    @Autowired
    private UserAdminService userAdminService;
    @PostMapping("/list")
    @ApiOperation(value = "用户列表")
    public Result<PageResp<AdminUserVO>> Userlist(@Valid @RequestBody UserAdminDTO userAdminDTO)
    {
        return userAdminService.Userlist(userAdminDTO);
    }
    @ApiOperation(value = "封禁/解禁用户")
    @GetMapping("/banUser")
    public Result<BanUserStatusVO> BanUser(@NotNull(message = "用户ID不能为空") @RequestParam(value = "userId",required = true)Long userId)
    {
        return userAdminService.BanUser(userId);
    }
    @ApiOperation(value = "查看用户详情")
    @GetMapping("detailUser")
    public Result<DetailUserVO> DetailUser(@NotNull(message = "用户ID不能为空") @RequestParam(value = "userId",required = true) Long userId)
    {
        return userAdminService.getDetailUser(userId);
    }
    @ApiOperation(value = "修改用户信息")
    @PostMapping("/updateUser")
    public Result<String> UpdateUser(@Valid @RequestBody UpdateUserDTO updateUserDTO)
    {
        return userAdminService.UpdateUser(updateUserDTO);
    }
    @ApiOperation(value="添加用户")
    @PostMapping("/addUser")
    public Result<String> AddUser(@Valid @RequestBody AddUserDTO addUserDTO)
    {
        return userAdminService.AddUser(addUserDTO);
    }
    @ApiOperation(value = "删除用户")
    @GetMapping("/deleteUser")
    public Result<String> DeleteUser(@NotNull(message = "用户ID不能为空")@RequestParam(value = "userId",required = true)Long userId)
    {
        return userAdminService.DeleteUser(userId);
    }
    @ApiOperation(value = "批量删除用户")
    @PostMapping("/deleteUsersList")
    public Result<String> DeleteUsersList(@Valid @RequestBody DeleteUserDTO deleteUserDTO)
    {
        return userAdminService.DeleteUsersList(deleteUserDTO);
    }

}
