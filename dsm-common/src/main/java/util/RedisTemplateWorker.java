package util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// 封装的 Redis 工具类
@RequiredArgsConstructor
@Component
public class RedisTemplateWorker {

    private final StringRedisTemplate stringRedisTemplate;

    // 起始时间戳（2021-04-03 21:30:30 UTC）
    public static final long BEGIN_TIMESTAMP = 1617485430L;
    // 序列号位数（最多支持 2^32 = 42亿/天/前缀，足够秒杀场景）
    public static final long COUNT_BITS = 32;

    /**
     * 全局唯一ID生成器（修复版）
     * @param keyPrefix 业务前缀（如"SECKILL_ORDER"）
     * @return 全局唯一ID（格式：时间戳(32位) + 序列号(32位)）
     */
    public long createId(String keyPrefix) {
        // 1. 生成时间戳（相对于起始时间的秒数）
        LocalDateTime now = LocalDateTime.now();
        long epochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = epochSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号（按天+前缀自增，避免跨天重复）
        // 2.1 拼接Redis自增Key（格式：icr:前缀:yyyy:MM:dd）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String incrKey = "icr:" + keyPrefix + ":" + date;
        // 2.2 自增序列号（核心：接收自增结果）
        // increment(key, 1)  对 Redis 中incrKey这个键对应的值执行原子自增：
        //- 如果incrKey不存在，先创建该键并赋值为0，再自增1（最终值为1）；
        //- 如果incrKey已存在，直接将值 + 1（比如当前是5，执行后变成6）；
        //- 第二个参数1表示每次自增步长（也可设为 2、10 等，秒杀场景用 1 即可）；
        //- 返回自增结果
        Long sequence = stringRedisTemplate.opsForValue().increment(incrKey, 1);
        if (sequence == null) {
            throw new RuntimeException("RedisTemplateWorker.createId:Redis自增序列号失败");
        }

        // 3. 拼接最终ID（时间戳左移32位 + 实际序列号）
        // 注意：sequence需做位运算限制（防止超过32位），避免溢出
        // sequence & ((1L << 32) - 1) 强制序列号只保留低 32 位，
        // 即使序列号超过 32 位（比如 4294967296），也会被截断为 0
        return (timestamp << COUNT_BITS) | (sequence & ((1L << COUNT_BITS) - 1));
    }

//    // 测试方法（验证ID唯一性）
//    public static void main(String[] args) {
//        // 模拟不同时间/序列号生成ID
//        long t1 = 1000; // 模拟时间戳
//        long s1 = 1;    // 模拟序列号1
//        long s2 = 2;    // 模拟序列号2
//        long id1 = (t1 << 32) | (s1 & ((1L << 32) - 1));
//        long id2 = (t1 << 32) | (s2 & ((1L << 32) - 1));
//        System.out.println(id1); // 不同序列号→不同ID
//        System.out.println(id2);
//    }
}