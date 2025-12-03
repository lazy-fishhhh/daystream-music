package com.future.login.util;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import util.JwtUtil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static constants.RedisCacheConstants.CACHE_NORMAL_TTL;
import static constants.RedisCacheConstants.TOKEN_USER_PREFIX;

/**
 * token刷新工具类
 */
@Component
public class RefreshUtil {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String cheak(Long userId,String currenttoken)
    {
        String key=TOKEN_USER_PREFIX + userId;
        //通过userId从redis中获取token的过期时间
        Long expire = stringRedisTemplate.getExpire(key);
        if(expire!=null&&expire<1200L)
        {
            //如果token存在且小于20分钟，就刷新
            return refresh(userId,currenttoken,key);
        }
        return null;
    }
    public String refresh(Long userId,String currenttoken,String key)
    {
        //判断当前token是否已经和redis中的token相等
        String token = stringRedisTemplate.opsForValue().get(key);
        if(token==null|| Objects.equals(token,currenttoken))
        {
            return null;
        }
        //生成新的token
        String newtoken = JwtUtil.createJWT(userId.toString());
        //存入reids
        stringRedisTemplate.opsForValue().set(key,newtoken,CACHE_NORMAL_TTL, TimeUnit.SECONDS);
        return newtoken;
    }
}
