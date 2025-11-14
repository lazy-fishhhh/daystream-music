package com.future.login.service.impl;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.domain.dto.CaptchaDTO;
import com.future.login.domain.vo.CaptchaVO;
import com.future.login.service.CaptchaService;
import entity.pojo.util.Result;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static constants.RedisCacheConstants.USER_CAPTCHA;


@Service
public class CaptchaServiceImpl implements CaptchaService {
    //验证码过期时间五分钟
    private static final long EXPECT_SECONDS=300L;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public Result<CaptchaVO> creatCaptcha() {
        //生成4位验证码
        String captcha = RandomUtil.randomString(4);
        //设置图形验证码的长 宽 验证码长度 干扰线数
        LineCaptcha lineCaptcha=new LineCaptcha(110,40,4,10);
        //设置图片颜色
        lineCaptcha.setBackground(new Color(230,244,255));
        //生成图片
        Image image=lineCaptcha.createImage(captcha);
        //转成base64
        String jpg = ImgUtil.toBase64(image, "jpg");

        /*
         * 返回验证码对象
         */
        //生成唯一标识
        String uuid= IdUtil.fastSimpleUUID();
        CaptchaVO captchaVO=new CaptchaVO();
        captchaVO.setCaptchaUUId(uuid);
        captchaVO.setContent(captcha);
        captchaVO.setCaptchaBase64Image("data:image/png;base64,"+jpg);
        captchaVO.setExpireSeconds(EXPECT_SECONDS);
        //存入reids
        stringRedisTemplate.opsForValue().set(USER_CAPTCHA+":"+uuid,captcha);
        stringRedisTemplate.expire(USER_CAPTCHA+":"+uuid,EXPECT_SECONDS, TimeUnit.SECONDS);
        return Result.ok(captchaVO);
    }

    @Override
    public Result checkCaptcha(CaptchaDTO captchaDTO) {
        if (StringUtils.isBlank(captchaDTO.getCaptchaUUId()) || StringUtils.isBlank(captchaDTO.getCaptcha())) {
            return Result.fail("请输入正确验证码");
        }
       //从redis中获取
        String key=USER_CAPTCHA+":"+captchaDTO.getCaptchaUUId();
        String captcha = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isBlank(captcha))
        {
            return Result.fail("验证码已过期,请刷新重试");
        }
        //如果查到了
        if(!Objects.equals(captchaDTO.getCaptcha(),captcha))
        {
            return Result.fail("请输入正确的验证码");
        }
        stringRedisTemplate.delete(key);
        return Result.ok();
    }
}
