package com.future.login.handler;

import cn.hutool.json.JSONUtil;
import entity.pojo.util.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import util.WebUtils;

import java.io.IOException;

@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String jsonStr = JSONUtil.toJsonStr(Result.fail(HttpStatus.FORBIDDEN.value(), "您的权限不足"));
        WebUtils.renderString(response,jsonStr);
    }
}
