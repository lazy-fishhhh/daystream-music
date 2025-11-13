package com.future.login.service.impl;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.domain.LoginUserDetails;
import com.future.login.domain.dto.LoginEmailDTO;
import com.future.login.exception.EmailNoFoundException;
import com.future.login.service.CaptchaService;
import com.future.login.service.LoginService;
import entity.pojo.util.Result;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import util.CacheClient;
import util.JwtUtil;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static code.UserErrorCode.PARAM_ERROR;
import static constants.RedisCacheConstants.*;


@Service

public class LoginServiceImpl implements LoginService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CaptchaService captchaService;
    @Override
    public Result LoginByEmail(LoginEmailDTO emailDTO) {
        return authenticateAndGenerateToken(emailDTO);
    }
    private  Result authenticateAndGenerateToken(LoginEmailDTO emailDTO) {
      String email=emailDTO.getEmail();
      String password=emailDTO.getPassword();
      String captcha=emailDTO.getCaptcha();
      if(StringUtils.isBlank(email))
      {
          return Result.fail("邮箱不能为空");
      }
      //校验验证码
      Result result1= captchaService.checkCaptcha(emailDTO);
      if(!result1.getSuccess())
      {
          return Result.fail(PARAM_ERROR.getCode(),result1.getErrorMsg());
      }
      UsernamePasswordAuthenticationToken us = new UsernamePasswordAuthenticationToken(email,password);
      Authentication authentication;
      try {
          authentication = authenticationManager.authenticate(us);
      }
      catch (EmailNoFoundException e) {
          return Result.fail("邮箱未注册");
      } catch (BadCredentialsException e) {
          return Result.fail("密码错误");
      } catch (AccountExpiredException e) {
          return Result.fail("账户已被禁用");
      }
      catch (AuthenticationException e) {
          return Result.fail("登录失败：" + e.getMessage());
      }
      //认证成功，获取用户详情
      LoginUserDetails principal = (LoginUserDetails) authentication.getPrincipal();
      Long userId = principal.getUser().getId();
      //根据userId生成Jwt
      String jwt = JwtUtil.createJWT(userId.toString());
      stringRedisTemplate.opsForValue().set(
              TOKEN_USER_PREFIX + userId,
              jwt,
              CACHE_NORMAL_TTL, TimeUnit.SECONDS
      );

      //将LoginUserDetail存入redis
      //存的是对象需要序列化
      try {
          String loginUserJson = objectMapper.writeValueAsString(principal);
          stringRedisTemplate.opsForValue().set(
                  USER_INFO_PREFIX + userId,
                  loginUserJson,
                  CACHE_NORMAL_TTL, TimeUnit.SECONDS
          );
      } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
      }
      HashMap<String, String> result = new HashMap<>();
      result.put("token", jwt);
      return Result.ok(result);
    }
}
