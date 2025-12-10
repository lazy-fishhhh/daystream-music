package com.future.login.service.impl;



import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.domain.LoginUserDetails;
import com.future.login.domain.dto.FogetDTO;
import com.future.login.domain.dto.LoginEmailDTO;
import com.future.login.exception.AccountDisabledException;
import com.future.login.exception.DeactivatedUserException;
import com.future.login.exception.EmailNoFoundException;
import com.future.login.mapper.PermissionMapper;
import com.future.login.mapper.RoleMapper;
import com.future.login.mapper.UserMapper;
import com.future.login.mapper.UserRoleMapper;
import com.future.login.service.CaptchaService;
import com.future.login.service.LoginService;
import com.future.login.util.SecurityUtil;
import constants.RegexPatterns;
import entity.pojo.info.User;
import entity.pojo.info.UserRole;
import entity.pojo.util.Result;
import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import util.JwtUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static code.UserErrorCode.PARAM_ERROR;
import static constants.RedisCacheConstants.*;


@Service

public class LoginServiceImpl implements LoginService {
    @Getter
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
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PermissionMapper permissionMapper;
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

    //通过openId查找本地用户，如果有登录，没有创建
    @Override
    public String loginByQQ(String openId,Map<String, String> userInfo) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getQqOpenId, openId);
        User user = userMapper.selectOne(queryWrapper);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            // 新用户：创建
            user = createNewQQUser(userInfo,openId);
            userMapper.insert(user);
            // 创建默认用户角色
            assignDefaultRole(user.getId());
        } else {
            //老用户：检查状态
            checkUserStatus(user);
            // 更新最后登录时间和头像
            updateUserLoginInfo(user, userInfo);
        }

        //公共流程：生成令牌和缓存
        String jwt = generateAndCacheUserToken(user);
        return jwt;
    }

    /**
     * 创建新的QQ用户
     */
    private User createNewQQUser(Map<String, String> userInfo,String openId) {
        User newUser = new User();
        // 基础信息
        newUser.setQqOpenId(userInfo.get("openid"));
        newUser.setUsername(userInfo.get("nickname"));
        newUser.setAvatar(userInfo.get("figureurl_qq_2"));
        newUser.setGender(changeSex(userInfo.get("gender")));
        //设置唯一标识
        newUser.setQqOpenId(openId);
        newUser.setPassword(null);
        newUser.setUserStatus(1);
        // 生成唯一邀请码
        newUser.setReferralCode(generateUniqueReferralCode());
        return newUser;
    }

    /**
     * 检查用户状态
     */
    private void checkUserStatus(User user) {
        if (user.getUserStatus() == 2) {
            throw new DeactivatedUserException("用户不存在");
        }
        if (user.getUserStatus() == 0) {
            throw new AccountDisabledException("封禁用户禁用");
        }
    }

    /**
     * 更新用户登录信息
     */
    private void updateUserLoginInfo(User user, Map<String, String> userInfo) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("qq_open_id", user.getQqOpenId())
                .set("last_login_time", LocalDateTime.now())
                .set("avatar", userInfo.get("figureurl_qq_2"));
        userMapper.update(null, wrapper);
        user.setLastLoginTime(LocalDateTime.now());
        user.setAvatar(userInfo.get("figureurl_qq_2"));
    }

    /**
     * 为用户分配默认角色
     */
    private void assignDefaultRole(Long userId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(1L); // 普通用户角色
        userRoleMapper.insert(userRole);
    }

    /**
     * 生成JWT令牌并缓存用户信息
     */
    private String generateAndCacheUserToken(User user) {
        // 1. 获取权限和角色
        Long userId = user.getId();
        List<String> permission = permissionMapper.selectPermissionByUserId(userId);
        List<String> roles = roleMapper.selectRoleByUserId(userId);
        List<String> authorities = new ArrayList<>();
        authorities.addAll(permission);
        authorities.addAll(roles.stream().map(role -> "ROLE_" + role).collect(Collectors.toList()));

        // 2. 生成JWT
        String jwt = JwtUtil.createJWT(userId.toString());
        stringRedisTemplate.opsForValue().set(
                TOKEN_USER_PREFIX + userId,
                jwt,
                CACHE_NORMAL_TTL, TimeUnit.SECONDS
        );

        // 3. 缓存用户详情
        LoginUserDetails loginUserDetails = new LoginUserDetails(user, authorities);
        try {
            String loginUserJson = objectMapper.writeValueAsString(loginUserDetails);
            stringRedisTemplate.opsForValue().set(
                    USER_INFO_PREFIX + userId,
                    loginUserJson,
                    CACHE_NORMAL_TTL, TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化用户信息失败", e);
        }
        return jwt;
    }

    /**
     * 生成唯一邀请码
     */
    private String generateUniqueReferralCode() {
        while (true) {
            String code = RandomUtil.randomString(6);
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getReferralCode, code);
            if (userMapper.selectOne(wrapper) == null) {
                return code;
            }
        }
    }
    private Integer changeSex(String sex) {
        switch (sex) {
            case "男":
                return 1;
            case "女":
                return 2;
            default:
                return 0;
        }
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
