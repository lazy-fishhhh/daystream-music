package util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    // 使用 generateKey() 方法生成的密钥
    public static final String JWT_KEY = "tO9rN+5QZ8e7wF3pL2mK5jR8sT1vU4xY7iH0kG3fJ6h";

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
        byte[] encodedKey = Base64.getDecoder().decode(JWT_KEY);
        return Keys.hmacShaKeyFor(encodedKey);
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
            System.out.println(b.encode("123456Aa@"));
            // $2a$10$MrfwYY2cFGg/tM5qefy9ROEzFPksUReBLYXB3ML.MhqwnKm9ssDQu
        }
    */
}
