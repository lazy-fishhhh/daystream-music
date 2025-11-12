package com.future.login.handler;

import cn.hutool.json.JSONUtil;

import com.future.login.exception.AccountDisabledException;
import com.future.login.exception.EmailNoFoundException;
import entity.pojo.util.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import util.WebUtils;

import java.io.IOException;
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String message;
       if(authException instanceof AccountDisabledException) {
           message = "禁用注销账号";
       }
       else if(authException instanceof EmailNoFoundException) {
           message = "邮箱未注册";
       }
       else if(authException instanceof BadCredentialsException)
       {
           message="密码错误";
       }
       else {
            message = "用户认证失败请重新登录";
        }
        String json = JSONUtil.toJsonStr(Result.fail(message));
        WebUtils.renderString(response, json);
    }
}
