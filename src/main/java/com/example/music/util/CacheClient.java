package com.example.music.util;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.music.constants.RedisCacheConstants.CACHE_NULL_TTL_RANDOM;


/**
 * 缓存工具类
 *
 *
 *
 * @Slf4j // 自动生成 log 对象
 * public class CacheClient {
 *     public void getCache() {
 *         log.info("获取缓存数据"); // 直接使用 log 调用日志方法
 *     }
 * }
 */
@Component
@Slf4j
public class CacheClient {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "cache:lock:";

//构建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

//将任意Java对象序列化为JSON格式后存入Redis，并设置过期时间
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

//设置逻辑过期时间
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入Redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

//缓存穿透工具类(缓存穿透问题: 缓存穿透是指客户端请求的数据在缓存中和数据库中都不存在,这样缓存永远不会生效,这些请求都会打到数据库)
    public <T,I> T queryWithPassThrough(String keyPrefix, I id, Class<T> type,
                                        Function<I,T> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        Long ti = randomObjectTimes(time); //解决缓存雪崩问题

        //从Redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断商铺是否存在
        if (StrUtil.isNotBlank(json)) {
            //若商铺信息在redis缓存中存在,直接将商铺信息返回给前端
            return JSONUtil.toBean(json, type);
        }
        //解决缓存穿透: 判断缓存命中的是否为空值
        if (json != null) {
            //返回错误信息
            return null;
        }

        //若商铺信息在redis缓存中不存在,则再去查询数据库 若信息存在,返回前端并存入redis缓存中
        T t  = dbFallback.apply(id);
        if (t == null) {
            //若不存在,直接返回错误信息 解决缓存穿透问题:将空值写入Redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL_RANDOM, TimeUnit.MINUTES);
            return null;
        }
        //若信息存在,返回前端并存入redis缓存中
        this.set(key,t,ti,unit);
        return t;
    }

//缓存雪崩:解决缓存雪崩工具类(随机时间解决) 缓存雪崩: 是指在同一时段大量的缓存key同时失效或者Redis服务宕机,导致瞬时DB请求量大,引起DB崩溃
    //对象随机时间
    public long randomObjectTimes(Long time){
        return time +  ThreadLocalRandom.current().nextLong(1,11);
    }
    //空值随机时间
    public long randomNullTimes(Long time){
        return time + ThreadLocalRandom.current().nextLong(1,4);
    }


//缓存击穿:利用逻辑过期时间解决对信息查询的缓存击穿问题
    public <T,I> T queryWithLogicalExpire(String keyPrefix,I id,Class<T> type,
                                          Function<I,T> dbFallback,Long time,TimeUnit unit){

        String key = keyPrefix + id;
        Long ti = time + randomObjectTimes(time);

        //从Redis中查询 数据 缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断 数据 是否存在
        if (StrUtil.isBlank(json)) {
            //若 数据 信息在redis缓存未命中,返回null(错误信息)
            return null;
        }
        //1.若命中,需先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        T t = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //2.判断缓存是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //3.1未过期,直接返回未过期的商铺信息
            return t;
        }
        //3.2若过期,需要重建缓存
        String lockKey = LOCK_PREFIX + key;
        //4.1重建缓存,获取互斥锁
        boolean isLock = tryLock(lockKey);
        //4.2判断互斥锁的获取是否成功
        if(isLock){
            //4.4若成功,开启独立线程,实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    //先查询数据库
                    T t1 = dbFallback.apply(id);
                    //写入Redis
                    this.setWithLogicalExpire(key,t1,ti,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally{
                    //释放互斥锁
                    tryUnLock(lockKey);
                }
            });
        }
        //4.4若失败,返回过期的信息
        return t;
    }

    //获取互斥锁
    private boolean tryLock(String key){
        //使用Redis中的setnx命令实现互斥锁
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        boolean f = BooleanUtil.isTrue(flag);
        return f;
    }
    //释放互斥锁
    private void tryUnLock(String key){
        stringRedisTemplate.delete(key);
    }


    public void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 从Redis中查询列表数据
     * @param keyPrefix 缓存键前缀
     * @param id 缓存键后缀
     * @param type 列表元素类型
     * @return 列表数据
     */
    public <T> List<T> queryWithListForPage(String keyPrefix, Long id,int startIndex,int endIndex, Class<T> type, Function<Long,List<T>> dbFallback){
        String key = keyPrefix + id;
        List<String> range = stringRedisTemplate.opsForList().range(key, startIndex, endIndex);

        //缓存命中，返回集合
        if (range != null && !range.isEmpty()) {
            log.info("从redis中查的");
            return range.stream().map(s -> JSONUtil.toBean(s, type)).collect(Collectors.toList());
        }
        //缓存未命中，查询数据库
        //重建缓存
        boolean b = tryLock(key);
        log.info("tryLock key:{}",b);
        if(b){
            try {
              List<T> list  = dbFallback.apply(id);
                log.info("list size:"+list);
                //数据库查询为空，返回空集合
                if (list == null || list.isEmpty()) {
                    return Collections.emptyList();
                }
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    //写入Redis
                    stringRedisTemplate.opsForList().rightPushAll(key , list.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList()));
                    //设置过期时间
                    stringRedisTemplate.expire(key , 7L, TimeUnit.DAYS);
                });
                return list;
            }catch (Exception e) {
                log.error(e.getMessage());
                return Collections.emptyList();
            } finally{
                    //释放互斥锁
                    tryUnLock(key);
            }
        }
        return Collections.emptyList();
    }



    public void delete(String cacheKey) {
        stringRedisTemplate.delete(cacheKey);
    }
}
