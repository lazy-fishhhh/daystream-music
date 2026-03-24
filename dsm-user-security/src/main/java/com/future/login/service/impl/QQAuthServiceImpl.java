package com.future.login.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.future.login.service.LoginService;
import com.future.login.service.QQAuthService;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service
public class QQAuthServiceImpl implements QQAuthService {

    @Value("${qq.auth.app-id}")
    private String appId;

    @Value("${qq.auth.app-key}")
    private String appKey;

    @Value("${qq.auth.redirect-uri}")
    private String redirectUri;

    @Value("${qq.auth.authorize-url}")
    private String authorizeUrl;

    @Value("${qq.auth.access-token-url}")
    private String accessTokenUrl;

    @Value("${qq.auth.openid-url}")
    private String openidUrl;

    @Value("${qq.auth.user-info-url}")
    private String userInfoUrl;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoginService loginService;

    /**
     * 第一步：生成QQ登录URL
     */
    @Override
    public Result<String> getAuthorizationUrl() {
        try {
            // 1. 生成随机state参数防止CSRF攻击
            String state = UUID.randomUUID().toString().replace("-", "").substring(0, 24);

            // 2. 将state存入Redis，有效期10分钟
            String stateKey = "qq:state:" + state;
            stringRedisTemplate.opsForValue().set(stateKey, "valid", 10, TimeUnit.MINUTES);

            // 3. 拼接QQ授权URL（编码redirectUri）
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
            String url = String.format("%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                    authorizeUrl, appId, encodedRedirectUri, state);

            return Result.ok(url);
        } catch (Exception e) {
            return Result.fail("生成QQ登录链接失败：" + e.getMessage());
        }
    }
 @Override
    /**
     * 第二步：处理QQ回调，获取用户信息
     */
    public void handleCallback(String code, String state, HttpServletResponse response) {
     try {
         // 1. 验证state参数（防CSRF攻击）
         String stateKey = "qq:state:" + state;
         String cachedState = stringRedisTemplate.opsForValue().get(stateKey);

         if (cachedState == null || !cachedState.equals("valid")) {
             log.warn("无效的state参数，可能已过期或被篡改");
         }

         // 2. 验证通过后删除state
         stringRedisTemplate.delete(stateKey);

         // 3. 使用code换取access_token
         String accessToken = getAccessToken(code);

         // 4. 使用access_token获取openid
         String openid = getOpenId(accessToken);

         // 5. 使用access_token和openid获取用户信息
         Map<String, String> userInfo = getUserInfo(accessToken, openid);

         // 6. 处理本地用户（创建或更新）
         String token = loginService.loginByQQ(openid, userInfo);

         //生成随机code
         String tempCode = UUID.randomUUID().toString().replace("-", "");

         // 把token放入redis(一分钟有效)，使用code换取token
         stringRedisTemplate.opsForValue().set(
                 "qq:temp:" + tempCode,
                 token,
                 60,
                 TimeUnit.SECONDS
         );
         // 7. 重定向
         response.sendRedirect("https://www.histweilai.top/?tempCode=" + tempCode);
     } catch (Exception e) {
         // 捕获所有异常，返回失败结果
         log.error("QQ登录回调处理失败：" + e.getMessage());
         try {
             response.sendRedirect("https://www.histweilai.top/UserAuth");
         } catch (IOException ex) {
             throw new RuntimeException(ex);
         }
     }
 }

    /**
     * 使用tempCode换取token
     */
    @Override
    public Result<HashMap<String,String>> getToken(String code)
    {
        String key="qq:temp:"+code;
        String token=stringRedisTemplate.opsForValue().get(key);
        if(token==null)
        {
            return Result.fail("临时码过期或无效");
        }else
        {
            //用后即删
            stringRedisTemplate.delete(key);
        }
        HashMap<String,String> result=new HashMap<>();
        result.put("token",token);
        return Result.ok(result);
    }

    /**
     * 使用授权码换取access_token
     */
    private String getAccessToken(String code) throws Exception {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String url = String.format("%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                accessTokenUrl, appId, appKey, code, encodedRedirectUri);

        String response = restTemplate.getForObject(url, String.class);
        Map<String, String> params = parseQueryString(response);
        String accessToken = params.get("access_token");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new RuntimeException("获取access_token失败：" + response);
        }

        return accessToken;
    }

    /**
     * 使用access_token获取openid
     */
    private String getOpenId(String accessToken) throws Exception {
        String url = openidUrl + "?access_token=" + accessToken;
        String response = restTemplate.getForObject(url, String.class);

        try {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            String jsonStr = response.substring(start, end);
            JSONObject json = JSON.parseObject(jsonStr);
            String openid = json.getString("openid");

            if (openid == null || openid.isEmpty()) {
                throw new RuntimeException("获取openid失败：" + response);
            }

            return openid;
        } catch (Exception e) {
            throw new RuntimeException("解析openid响应失败：" + response, e);
        }
    }

    /**
     * 获取QQ用户信息
     */
    private Map<String, String> getUserInfo(String accessToken, String openid) throws Exception {
        String url = String.format("%s?access_token=%s&oauth_consumer_key=%s&openid=%s",
                userInfoUrl, accessToken, appId, openid);

        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = JSON.parseObject(response);

        if (json.getInteger("ret") != 0) {
            throw new RuntimeException("获取用户信息失败：" + response);
        }

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("nickname", json.getString("nickname"));
        userInfo.put("avatar_30", json.getString("figureurl"));
        userInfo.put("avatar_50", json.getString("figureurl_1"));
        userInfo.put("avatar_100", json.getString("figureurl_2"));
        userInfo.put("gender", json.getString("gender"));
        userInfo.put("province", json.getString("province"));
        userInfo.put("city", json.getString("city"));

        return userInfo;
    }

    /**
     * 解析URL查询字符串
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }
}