package com.future.login.service;

import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;

public interface QQAuthService {
    Result<HashMap<String,String>> getToken(String code);
    Result<String> getAuthorizationUrl();
    void handleCallback(String code, String state, HttpServletResponse response);
}
