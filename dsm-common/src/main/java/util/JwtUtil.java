package util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类
 */
public class JwtUtil {

    // 有效期为
    public static final Long JWT_TTL = 60 * 60 * 1000*7L; // 1小时

    /**
     * JWT 签名密钥（Base64 编码，HS256 要求解码后至少 32 字节）。
     * 由 jwt.secret 配置项注入，未配置时启动会自动生成临时密钥。
     */
    private static volatile String jwtKey;

    /**
     * 由 {@link JwtKeyInitializer} 在容器启动时调用
     */
    public static void setJwtKey(String key) {
        if (key != null && !key.isBlank()) {
            jwtKey = key;
        }
    }

    public static String getUUID() {
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        return token;
    }

    /**
     * 生成JWT
     * @param subject token中要存放的数据
     * @return JWT字符串
     */
    public static String createJWT(String subject) {
        return createJWT(subject, null);
    }

    /**
     * 生成JWT
     * @param subject token中要存放的数据
     * @param ttlMillis token超时时间（毫秒），null则使用默认值
     * @return JWT字符串
     */
    public static String createJWT(String subject, Long ttlMillis) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        SecretKey secretKey = generalKey();

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        if (ttlMillis == null) {
            ttlMillis = JWT_TTL;
        }

        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);

        return Jwts.builder()
                .setId(getUUID())
                .setSubject(subject)
                .setIssuer("lee")
                .setIssuedAt(now)
                .setExpiration(expDate)
                .signWith(secretKey, signatureAlgorithm)
                .compact();
    }

    /**
     * 解析JWT
     * @param jwt JWT字符串
     * @return Claims对象
     * @throws JwtException 如果JWT无效或签名不匹配
     */
    public static Claims parseJWT(String jwt) throws JwtException {
        try {
            SecretKey secretKey = generalKey();
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();
        } catch (ExpiredJwtException e) {
            System.err.println("JWT已过期: " + e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            System.err.println("不支持的JWT格式: " + e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            System.err.println("JWT格式错误: " + e.getMessage());
            throw e;
        } catch (SignatureException e) {
            System.err.println("JWT签名验证失败: " + e.getMessage());
            System.err.println("可能原因: 密钥不匹配或JWT被篡改");
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("JWT为空或参数错误: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 生成加密后的秘钥
     * @return 密钥对象
     */
    public static SecretKey generalKey() {
        byte[] encodedKey = Base64.getDecoder().decode(resolveKey());
        return Keys.hmacShaKeyFor(encodedKey);
    }

    /**
     * 解析密钥：优先使用 Spring 注入的 jwt.secret，
     * 其次读取 JWT_SECRET 环境变量 / 系统属性，
     * 都没有时生成一个临时密钥（仅用于本地开发，重启后已签发的 token 会全部失效）。
     */
    private static String resolveKey() {
        String key = jwtKey;
        if (key == null || key.isBlank()) {
            key = System.getProperty("jwt.secret");
        }
        if (key == null || key.isBlank()) {
            key = System.getenv("JWT_SECRET");
        }
        if (key == null || key.isBlank()) {
            key = generateKey();
            System.err.println("[JwtUtil] 未配置 jwt.secret（环境变量 JWT_SECRET），"
                    + "已自动生成临时密钥，应用重启后所有已签发的 token 将失效。");
        }
        jwtKey = key;
        return key;
    }

    /**
     * 生成新的安全密钥
     * @return Base64编码的密钥字符串
     */
    public static String generateKey() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /*
       public static void main(String[] args) {
            BCryptPasswordEncoder b = new BCryptPasswordEncoder();
            System.out.println(b.encode("your-password"));
        }
    */

}
