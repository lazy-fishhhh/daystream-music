package com.future.login.util;

import com.future.login.domain.LoginUserDetails;
import entity.pojo.info.User;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class SecurityUtil {
    // 获取当前登录用户的ID
    public static Long getCurrentUserId(){
        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return loginUser.getUser().getId();
    }
    // 获取当前登录用户
    public static User getCurrentUser(){
        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return  loginUser.getUser();
    }
    // 获取当前登录用户的权限
    public static List<String> getCurrentUserPermissions(){
        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return loginUser.getPermission();
    }
}
