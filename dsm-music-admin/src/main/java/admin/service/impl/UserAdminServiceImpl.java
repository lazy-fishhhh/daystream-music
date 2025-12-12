package admin.service.impl;

import admin.domain.dto.AddUserDTO;
import admin.domain.dto.DeleteUserDTO;
import admin.domain.dto.UpdateUserDTO;
import admin.domain.dto.UserAdminDTO;
import admin.domain.vo.AdminUserVO;
import admin.domain.vo.BanUserStatusVO;
import admin.domain.vo.DetailUserVO;
import admin.mapper.AdminUserMapper;
import admin.service.UserAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import constants.RegexPatterns;
import entity.pojo.info.User;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static constants.RedisCacheConstants.TOKEN_USER_PREFIX;

@Service
public class UserAdminServiceImpl implements UserAdminService {
    @Autowired
    private AdminUserMapper adminUserMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
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

    @Override
    public Result<BanUserStatusVO> BanUser(Long userId) {
        //查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId)
                .in(User::getUserStatus, 0, 1);
        User user = adminUserMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.fail("用户不存在");
        }else{
            //判断用户状态
            Integer userStatus = user.getUserStatus();
            BanUserStatusVO banUserStatusVO = new BanUserStatusVO();
            //如果用户被封禁，则解除
            if(userStatus==0)
            {
                user.setUserStatus(1);
                adminUserMapper.updateById(user);
                banUserStatusVO.setBan(false);
                banUserStatusVO.setMsg("已解封");
                return Result.ok(banUserStatusVO);
            }
            //如果用户未被封禁，则封禁
            else if(userStatus==1){
                //删除token
                String key=TOKEN_USER_PREFIX+userId;
                redisTemplate.delete(key);
                user.setUserStatus(0);
                adminUserMapper.updateById(user);
                banUserStatusVO.setBan(true);
                banUserStatusVO.setMsg("已封禁");
            }
        }
        return Result.fail("操作失败");
    }

    @Override
    public Result<DetailUserVO> getDetailUser(Integer id) {
        DetailUserVO detailUser = adminUserMapper.getDetailUser(id);
        if(detailUser==null)
        {
            return Result.fail("用户不存在");
        }
        return Result.ok(detailUser);
    }

    @Override
    public Result<String> UpdateUser(UpdateUserDTO updateUserDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, updateUserDTO.getId())
                        .in(User::getUserStatus, 0, 1);
        User user = adminUserMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        adminUserMapper.updateUser(updateUserDTO);
        return Result.ok("修改成功");
    }

    @Override
    public Result<String> AddUser(AddUserDTO addUserDTO) {
        //判断信息
        String username = addUserDTO.getUsername();
        String password = addUserDTO.getPassword();
        String email = addUserDTO.getEmail();
        String confirmPassword = addUserDTO.getConfirmPassword();
        if (!RegexPatterns.matches(username, RegexPatterns.USERNAME_REGEX)) {
            return Result.fail("用户名2-16位，请输入正确的用户名");
        }
        if (!RegexPatterns.matches(password, RegexPatterns.PASSWORD_REGEX)) {
            return Result.fail("密码8-20位,包含大写字母、数字和特殊字符,请输入正确的密码格式");
        }
        if(!Objects.equals(password, confirmPassword))
        {
            return Result.fail("密码不一致");
        }
        if (!RegexPatterns.matches(email, RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("邮箱格式不正确，请输入正确的邮箱");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email)
                .in(User::getUserStatus, 0,1);
        User user = adminUserMapper.selectOne(queryWrapper);
        if (user != null) {
            return Result.fail("该邮箱已经注册过了");
        }
        //如果没有问题，添加用户
        User user1 = new User();
        user1.setUsername(username);
        user1.setPassword(bCryptPasswordEncoder.encode(password));
        user1.setIntroduction(addUserDTO.getIntroduction());
        user1.setEmail(email);
        int insert = adminUserMapper.insert(user1);
        if(insert>0)
        {
            return Result.ok("添加成功");
        }
        return Result.fail("添加失败");
    }

    @Override
    public Result<String> DeleteUser(Long userId) {
        //删除单个用户
        //判断用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId)
                .in(User::getUserStatus, 0, 1);
        User user = adminUserMapper.selectOne(queryWrapper);
        if (user == null) {
              return Result.fail("用户不存在");
        }
        //删除用户,把用户的状态设置为注销
        user.setUserStatus(2);
        adminUserMapper.updateById(user);
        //删除token
        String key=TOKEN_USER_PREFIX+userId;
        redisTemplate.delete(key);
        return Result.ok("删除成功");
    }
    //批量删除用户
    @Override
    public Result<String> DeleteUsersList(DeleteUserDTO deleteUserDTO) {
        List<Long> userIds = deleteUserDTO.getUserId();
        //逐个删除用户
        userIds.stream()
                .forEach(userId -> {
                    LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(User::getId, userId)
                            .in(User::getUserStatus, 0, 1);
                    User user = adminUserMapper.selectOne(queryWrapper);
                    if (user != null) {
                        //删除用户,把用户的状态设置为注销
                        user.setUserStatus(2);
                        adminUserMapper.updateById(user);
                        //删除token
                        String key = TOKEN_USER_PREFIX + userId;
                        redisTemplate.delete(key);
                    }
                });
        return Result.ok("删除成功");
    }


}
