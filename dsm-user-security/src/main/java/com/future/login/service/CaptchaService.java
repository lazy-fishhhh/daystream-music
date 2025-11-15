package com.future.login.service;

import com.future.login.domain.dto.CaptchaDTO;
import com.future.login.domain.vo.CaptchaVO;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

/**
 * 验证码
 */
@Service
public interface CaptchaService {
    //生成图形验证码
    Result<CaptchaVO> creatCaptcha();
    //校验验证码
    Result checkCaptcha(CaptchaDTO captchaDTO);
}
