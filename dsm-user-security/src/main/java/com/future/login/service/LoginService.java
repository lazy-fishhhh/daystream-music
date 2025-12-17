package com.future.login.service;


import com.future.login.domain.dto.BindEmail;
import com.future.login.domain.dto.FogetDTO;
import com.future.login.domain.dto.LoginEmailDTO;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 登录
 */

public interface LoginService {
    //通过邮箱登录
    Result LoginByEmail(LoginEmailDTO emailDTO);
    //退出登录
    Result LoginOut();
    //忘记密码
    Result fogetPassword(FogetDTO fogetDTO);
    String loginByQQ(String openid,Map<String, String> userInfo);

    //Result bindEmail(BindEmail bindEmail);
}
