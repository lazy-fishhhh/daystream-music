package com.future.login.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.domain.LoginUserDetails;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import util.JwtUtil;

import java.io.IOException;
import java.util.Collections;

import static constants.RedisCacheConstants.TOKEN_USER_PREFIX;
import static constants.RedisCacheConstants.USER_INFO_PREFIX;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //获取token,从请求头中获取token
        String token = request.getHeader("Authorization");
        //对token进行一个判断，如果为空就放行
        if(!StringUtils.hasText(token))
        {
            filterChain.doFilter(request,response);
            return;
        }
        //解析token
        String userIdStr = null;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userIdStr = claims.getSubject();
            if (!StringUtils.hasText(userIdStr)) {
                throw new IllegalArgumentException("Token中用户ID为空");
            }
        } catch (Exception e) {
            // Token解析失败，直接放行
            filterChain.doFilter(request, response);
            return;
        }
        //根据userid从redis中获取信息
        // 3. 验证Redis中是否存在该token
        long userId = Long.parseLong(userIdStr);
        String redisTokenKey = TOKEN_USER_PREFIX + userId;
        String redisToken = stringRedisTemplate.opsForValue().get(redisTokenKey);

        // 检查Redis中的token是否存在且与请求中的token一致
        if (!StringUtils.hasText(redisToken) || !redisToken.equals(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. 从redis中获取权限信息封装到Authentication中
        String redisKey = USER_INFO_PREFIX + userId;
        String loginUserJson = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(loginUserJson)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 反序列化
        LoginUserDetails loginUser = objectMapper.readValue(loginUserJson, LoginUserDetails.class);

        // 处理权限信息（避免空指针）
        if (loginUser.getPermission() == null) {
            loginUser.setPermission(Collections.emptyList());
        }
        //存入SecurityContextHolder
        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(loginUser,null,loginUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request,response);
    }
}
