package admin.service.impl;

import admin.service.UpdateUserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.domain.LoginUserDetails;
import com.future.login.mapper.PermissionMapper;
import com.future.login.mapper.RoleMapper;
import com.future.login.mapper.UserMapper;
import entity.pojo.info.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static constants.RedisCacheConstants.CACHE_NORMAL_TTL;
import static constants.RedisCacheConstants.USER_INFO_PREFIX;

/**
 * 更新用户信息服务的实现类
 * 该类用于实现用户信息更新相关的业务逻辑
 */
@Slf4j
@Service
public class UpdateUserInfoServiceImpl implements UpdateUserInfoService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public void updateUserInfo(Long userId) {
        try {
            //从数据库中获取最新的用户信息
            User user = userMapper.selectById(userId);
            //获取用户的权限信息
            List<String> permission = permissionMapper.selectPermissionByUserId(user.getId());
            //获取用户角色
            List<String> roles = roleMapper.selectRoleByUserId(user.getId());
            List<String> userrole = roles.stream().map(
                            role -> "ROLE_" + role)
                    .collect(Collectors.toList());
            List<String> authorities = new ArrayList<>();
            //合并权限信息和角色信息
            authorities.addAll(permission);
            authorities.addAll(userrole);
            //构建新的LoginUserDetails对象
            LoginUserDetails loginUserDetails = new LoginUserDetails(user, authorities);
            //序列化
            String loginstr = objectMapper.writeValueAsString(loginUserDetails);
            String key=USER_INFO_PREFIX + userId;
            //获取之前缓存的过期时间
            Long expire = stringRedisTemplate.getExpire(key);
            if(expire!=null&&expire>0)
            {
                //保持原有的过期时间
                stringRedisTemplate.opsForValue().set(
                        key,
                        loginstr,
                        expire, TimeUnit.SECONDS
                );
                log.info("更新用户Redis缓存成功，userId: {}, 剩余时间: {}秒", userId, expire);
            }else{
                //默认过期时间
                stringRedisTemplate.opsForValue().set(
                        key,
                        loginstr,
                        CACHE_NORMAL_TTL, TimeUnit.SECONDS
                );
                log.info("更新用户Redis缓存成功（使用默认时间），userId: {}", userId);
            }
        }catch (Exception e)
        {
            log.error("更新用户Redis缓存失败，userId: {}", userId, e);
            throw new RuntimeException(e);
        }


    }
}
