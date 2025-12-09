package com.future.login.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class QQAuthServiceImpl {

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

    /*@Autowired
    private RestTemplate restTemplate;

   /* @Autowired
    private UserService userService;*/

    /**
     * 第一步：生成QQ登录URL
     */
    public String getAuthorizationUrl() {
        // 1. 生成随机state参数防止CSRF攻击
        String state = UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        // 2. 将state存入Redis，有效期10分钟
        String stateKey = "qq:state:" + state;
        stringRedisTemplate.opsForValue().set(stateKey, "valid", 10, TimeUnit.MINUTES);

        // 3. 拼接QQ授权URL（注意：redirectUri已在配置中编码）
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        String url = String.format("%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                authorizeUrl, appId, encodedRedirectUri, state);

        return url;
    }

    /**
     * 第二步：处理QQ回调，获取用户信息
     */
    /*public Map<String, Object> handleCallback(String code, String state) throws Exception {
        // 1. 验证state参数（防CSRF攻击）
        String stateKey = "qq:state:" + state;
        String cachedState = stringRedisTemplate.opsForValue().get(stateKey);

        if (cachedState == null || !cachedState.equals("valid")) {
            throw new RuntimeException("无效的state参数，可能已过期或被篡改");
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
        Map<String, Object> localUser = userService.processQQUser(openid, userInfo);

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("user", localUser);
        result.put("qq_openid", openid);
        result.put("qq_info", userInfo);

        return result;
    }*/

    /**
     * 使用授权码换取access_token
     */
    /*private String getAccessToken(String code) throws Exception {
        // 构建请求URL（注意：redirectUri已在配置中编码）
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String url = String.format("%s?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                accessTokenUrl, appId, appKey, code, encodedRedirectUri);

        // 发送GET请求
        String response = restTemplate.getForObject(url, String.class);

        // 解析响应（格式：access_token=YOUR_ACCESS_TOKEN&expires_in=7776000&refresh_token=YOUR_REFRESH_TOKEN）
        Map<String, String> params = parseQueryString(response);
        String accessToken = params.get("access_token");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new RuntimeException("获取access_token失败：" + response);
        }

        return accessToken;
    }*/

    /**
     * 使用access_token获取openid
     */
    /*private String getOpenId(String accessToken) throws Exception {
        String url = openidUrl + "?access_token=" + accessToken;

        // 发送GET请求（QQ返回的是JSONP格式）
        String response = restTemplate.getForObject(url, String.class);

        // 解析JSONP响应：callback({"client_id":"YOUR_APP_ID","openid":"YOUR_OPENID"});
        try {
            // 提取JSON部分
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            String jsonStr = response.substring(start, end);

            // 解析JSON
            JSONObject json = JSON.parseObject(jsonStr);
            String openid = json.getString("openid");

            if (openid == null || openid.isEmpty()) {
                throw new RuntimeException("获取openid失败：" + response);
            }

            return openid;
        } catch (Exception e) {
            throw new RuntimeException("解析openid响应失败：" + response, e);
        }
    }*/

    /**
     * 获取QQ用户信息
     */
    /*private Map<String, String> getUserInfo(String accessToken, String openid) throws Exception {
        String url = String.format("%s?access_token=%s&oauth_consumer_key=%s&openid=%s",
                userInfoUrl, accessToken, appId, openid);

        // 发送GET请求
        String response = restTemplate.getForObject(url, String.class);

        // 解析JSON响应
        JSONObject json = JSON.parseObject(response);

        if (json.getInteger("ret") != 0) {
            throw new RuntimeException("获取用户信息失败：" + response);
        }

        // 提取关键信息
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("nickname", json.getString("nickname"));
        userInfo.put("avatar_30", json.getString("figureurl"));       // 30x30头像
        userInfo.put("avatar_50", json.getString("figureurl_1"));     // 50x50头像
        userInfo.put("avatar_100", json.getString("figureurl_2"));    // 100x100头像
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