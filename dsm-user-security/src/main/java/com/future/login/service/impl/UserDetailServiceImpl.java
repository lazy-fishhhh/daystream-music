package com.future.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.future.login.domain.LoginUserDetails;
import com.future.login.exception.AccountDisabledException;
import com.future.login.exception.EmailNoFoundException;
import com.future.login.mapper.PermissionMapper;
import com.future.login.mapper.RoleMapper;
import com.future.login.mapper.UserMapper;
import entity.pojo.info.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
@Service
public class UserDetailServiceImpl implements UserDetailsService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Override
    public UserDetails loadUserByUsername(String emailorphone) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, emailorphone);
        User user = userMapper.selectOne(wrapper);
        //如果没找到，抛出未注册
        if(Objects.isNull(user))
        {
            throw new EmailNoFoundException("该邮箱未注册");
        }
        //如果是封禁用户，禁止登录
        if(user.getUserStatus()==0)
        {
            throw new AccountDisabledException("封禁用户禁用");
        }
        //获取用户的权限信息
        List<String> permission=permissionMapper.selectPermissionByUserId(user.getId());
        //获取用户角色
        List<String> roles = roleMapper.selectRoleByUserId(user.getId());
        List<String> authorities=new ArrayList<>();
        authorities.addAll(permission);
        authorities.addAll(roles);
        return new LoginUserDetails(user,authorities);
    }
}
