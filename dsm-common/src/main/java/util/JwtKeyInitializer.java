package util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动时把 jwt.secret 配置项注入到 {@link JwtUtil} 的静态密钥字段。
 * 该包已被各模块启动类的 @ComponentScan 覆盖。
 */
@Slf4j
@Component
public class JwtKeyInitializer {

    @Value("${jwt.secret:}")
    private String secret;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            secret = JwtUtil.generateKey();
            log.warn("未配置 jwt.secret（环境变量 JWT_SECRET），已自动生成临时密钥，"
                    + "应用重启后所有已签发的 token 将失效。生产环境请务必配置固定密钥。");
        }
        JwtUtil.setJwtKey(secret);
    }
}
