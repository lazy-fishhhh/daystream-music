package com.future.login.service;


import com.future.login.domain.dto.LoginEmailDTO;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;


@Service
public interface LoginService {
    //通过邮箱登录
    Result LoginByEmail(LoginEmailDTO emailDTO);
}
