package com.future.login.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.future.login.domain.dto.RejesterDTO;
import com.future.login.mapper.UserMapper;
import com.future.login.mapper.UserRoleMapper;
import com.future.login.service.RejesterService;
import constants.RegexPatterns;
import entity.pojo.info.User;
import entity.pojo.info.UserRole;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.awt.desktop.UserSessionEvent;
import java.util.Objects;

import static constants.RedisCacheConstants.USER_EMAILCAPTCHA;

@RequiredArgsConstructor
@Service
public class RejesterServiceImpl implements RejesterService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Override
    public Result rejester(RejesterDTO rejesterDTO) {
        //校验姓名、邮箱、密码格式
        String password = rejesterDTO.getPassword();
        String username = rejesterDTO.getUsername();
        String email = rejesterDTO.getEmail();
        String newPassword = rejesterDTO.getNewPassword();
        if (!RegexPatterns.matches(username, RegexPatterns.USERNAME_REGEX)) {
            return Result.fail("用户名2-16位，请输入正确的用户名");
        }
        LambdaQueryWrapper<User> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(User::getUsername, username);
        User user3 = userMapper.selectOne(lambdaQueryWrapper1);
        if (user3 != null) {
            return Result.fail("用户名已存在");
        }
        if (!RegexPatterns.matches(password, RegexPatterns.PASSWORD_REGEX)) {
            return Result.fail("密码8-20位,包含大写字母、数字和特殊字符,请输入正确的密码格式");
        }
        if (!RegexPatterns.matches(email, RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("邮箱格式不正确，请输入正确的邮箱");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(wrapper);
        if (!Objects.isNull(user)) {
            return Result.fail("该邮箱已经注册过了");
        }
        if (!Objects.equals(password, newPassword)) {
            return Result.fail("验证密码和新输入的密码不相同，请重新输入");
        }
        //校验验证码
        String key=USER_EMAILCAPTCHA+email;
        String captcha = stringRedisTemplate.opsForValue().get(key);
        if(Objects.isNull(captcha))
        {
            return Result.fail("验证码已过期，请重新获取");
        }
        if(!Objects.equals(captcha,rejesterDTO.getCaptcha()))
        {
            return Result.fail("请输入正确的验证码");
        }
        //校验成功，生成实体类
        User user1 = new User();
        user1.setUsername(username);
        user1.setPassword(bCryptPasswordEncoder.encode(password));
        user1.setEmail(email);
        //生成唯一邀请码
        while (true) {
            String code = RandomUtil.randomString(6);
            LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper();
            lambdaQueryWrapper.eq(User::getReferralCode, code);
            User user2 = userMapper.selectOne(lambdaQueryWrapper);
            if (user2 == null) {
                user1.setReferralCode(code);
                break;
            }
        }
        userMapper.insert(user1);

        // TODO 下面这些全都可以扔到消息队列
        //创建user_role表
        UserRole userRole=new UserRole();
        userRole.setUserId(user1.getId());
        //默认普通用户
        userRole.setRoleId(1L);
        userRoleMapper.insert(userRole);

        // 创建“我的喜欢”歌单
        userMapper.createPlaylist(user1.getId() , "我的喜欢", "https://picsum.photos/200/300", "我的喜欢");
        return Result.ok();
    }
}
