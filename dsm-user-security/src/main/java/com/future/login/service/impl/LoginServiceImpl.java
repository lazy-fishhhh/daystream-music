package com.future.login.service.impl;



import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.domain.LoginUserDetails;
import com.future.login.domain.dto.FogetDTO;
import com.future.login.domain.dto.LoginEmailDTO;
import com.future.login.exception.EmailNoFoundException;
import com.future.login.mapper.UserMapper;
import com.future.login.service.CaptchaService;
import com.future.login.service.LoginService;
import com.future.login.util.SecurityUtil;
import constants.RegexPatterns;
import entity.pojo.info.User;
import entity.pojo.util.Result;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import util.JwtUtil;

import java.util.HashMap;
import java.util.Objects;
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
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Override
    public Result LoginByEmail(LoginEmailDTO emailDTO) {
        return authenticateAndGenerateToken(emailDTO);
    }

    @Override
    public Result LoginOut() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        stringRedisTemplate.delete(USER_INFO_PREFIX+currentUserId);
        stringRedisTemplate.delete(TOKEN_USER_PREFIX+currentUserId);

        return Result.ok("退出成功");
    }

    @Override
    public Result fogetPassword(FogetDTO fogetDTO) {
        //验证新密码
        String password = fogetDTO.getPassword();
        String newPassword = fogetDTO.getNewPassword();
        String userEmail = fogetDTO.getEmail();
        if(!RegexPatterns.matches(userEmail,RegexPatterns.EMAIL_REGEX))
        {
            return Result.fail("请输入正确的邮箱");
        }
        if (!RegexPatterns.matches(password, RegexPatterns.PASSWORD_REGEX)) {
            return Result.fail("密码8-20位,包含大写字母、数字和特殊字符,请输入正确的密码格式");
        }
        if (!Objects.equals(password, newPassword)) {
            return Result.fail("验证密码和新输入的密码不相同，请重新输入");
        }
        //校验验证码
        String key=USER_EMAILCAPTCHA+userEmail;
        String captcha = stringRedisTemplate.opsForValue().get(key);
        if(Objects.isNull(captcha))
        {
            return Result.fail("验证码已过期，请重新获取");
        }
        if(!Objects.equals(captcha,fogetDTO.getCaptcha()))
        {
            return Result.fail("请输入正确的验证码");
        }
        UpdateWrapper<User> userUpdateWrapper=new UpdateWrapper<>();
        userUpdateWrapper.eq("email",userEmail);
        userUpdateWrapper.set("password",bCryptPasswordEncoder.encode(password));
        userMapper.update(null,userUpdateWrapper);
        return Result.ok();
    }

    private  Result authenticateAndGenerateToken(LoginEmailDTO emailDTO) {
      String email=emailDTO.getEmail();
      String password=emailDTO.getPassword();
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
