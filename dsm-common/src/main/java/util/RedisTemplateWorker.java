package util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


//  封装的 Redis 工具类
@RequiredArgsConstructor
@Component
public class RedisTemplateWorker {
    private final StringRedisTemplate StringRedisTemplate;

    // 起始时间戳
    public static final long BEGIN_TIMESTAMP = 1617485430L;
    // 序列号位数
    public static final long COUNT_BITS = 32;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 全局唯一ID生成器
     */
    public long createId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long epochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = epochSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 2.1 获取当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2 自增序列号
        stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);


        // 返回二进制形式
        // 1.左移时间戳，空出序列号的位置
        // 因为拼接完，想要把时间戳做高位，序列号做低位
        // 所以，视角从时间戳开始看。
        // 时间戳需要左移COUNT_BITS位(序列号的位数),
        // 把序列号的位置空出来用0填充(左移完空出来的位置默认是0呀)
        // 2.把序列号拼接到时间戳的后面
        // 由于默认空出来的32位是0，因此做或操作（|）就可以把序列号替换上去
        return timestamp<< COUNT_BITS |  COUNT_BITS;
    }

//    public static void main(String[] args) {
//        LocalDateTime then = LocalDateTime.of(2021, 4, 3,21, 30, 30);
//        long epochSecond = then.toEpochSecond(ZoneOffset.UTC);
//        System.out.println(epochSecond);
//    }
}
