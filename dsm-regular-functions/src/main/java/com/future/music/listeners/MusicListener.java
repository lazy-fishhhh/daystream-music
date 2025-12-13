package com.future.music.listeners;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.future.music.domain.vo.MusicTagVO;
import com.future.music.entity.message.MusicDownloadCountMsg;
import com.future.music.entity.message.MusicPlayReportMqMessage;
import com.future.music.mapper.MusicMapper;
import constants.MusicConstants;
import constants.RabbitConstants;
import constants.RedisCacheConstants;
import constants.SystemConstants;
import entity.pojo.music.Music;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MusicListener {

    @Resource
    private MusicMapper musicMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = RabbitConstants.MUSIC_DOWNLOAD_QUEUE, arguments = @Argument(name = "x-queue-mode", value = "lazy")),
            exchange = @Exchange(name = RabbitConstants.MUSIC_EXCHANGE),
            key = {RabbitConstants.ROUTING_KEY_MUSIC_DOWNLOAD}
    ))
    public void handleDownloadCount(MusicDownloadCountMsg msg) {
        try {
            Long musicId = msg.getMusicId();

            UpdateWrapper<Music> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", musicId)
                    .setSql("download_count = download_count + 1");
            musicMapper.update(null, updateWrapper);

        } catch (Exception e) {
            // 异常处理：可记录日志、重试消费（根据业务需求）
            throw new RuntimeException("更新下载量失败：" + e.getMessage());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = RabbitConstants.MUSIC_PLAY_QUEUE, arguments = @Argument(name = "x-queue-mode", value = "lazy")),
            exchange = @Exchange(name = RabbitConstants.MUSIC_EXCHANGE),
            key = {RabbitConstants.ROUTING_KEY_MUSIC_PLAY}
    ))
    public void handlePlayCount(MusicPlayReportMqMessage msg) {
        try {
            Long userId = msg.getUserId();
            List<MusicTagVO> tagList = msg.getTagList();
            LocalDate currentDate = msg.getCurrentDate();

            // 1. 处理用户标签听歌记录的Redis写入（核心异步任务）
            if (!tagList.isEmpty()) {
                String weekId = generateWeekId(currentDate);
                String cacheKey = MusicConstants.USER_LISTEN_TAG_WEEK + userId + ":" + weekId;

                for (MusicTagVO tag : tagList) {
                    // 读取Redis Hash值
                    String currentCountStr = (String) stringRedisTemplate.opsForHash().get(cacheKey, tag.getTagName());
                    long currentCount = 0;

                    // 安全转换，避免数据异常
                    if (StringUtils.hasText(currentCountStr)) {
                        try {
                            currentCount = Long.parseLong(currentCountStr);
                        } catch (NumberFormatException e) {
                            currentCount = 0;
                        }
                    }

                    // 累加后存入Redis
                    stringRedisTemplate.opsForHash().put(cacheKey, tag.getTagName(), String.valueOf(currentCount + 1));

                    // 首次存入时设置过期时间
                    if (currentCount == 0) {
                        stringRedisTemplate.expire(cacheKey, RedisCacheConstants.CACHE_QUESTIONNAIRE_TTL, TimeUnit.SECONDS);
                    }
                }
            }

            // 2.音乐总播放量统计（如更新music表的play_count字段）
            musicMapper.update(null, new UpdateWrapper<Music>()
                    .eq("id", msg.getMusicId())
                    .setSql("play_count = play_count + 1"));

        } catch (Exception e) {
            // 异常处理：记录日志、重试任务、死信队列兜底
            // 避免单个任务失败导致消费者阻塞
            e.printStackTrace();
        }
    }

    /**
     * 生成指定日期的周标识（周一为周初，周日为周末）
     * @param date 播放日期（null 则取当前日期）
     * @return 周标识，如 2025-49
     */
    public String generateWeekId(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        // 调整为本周一（确保整周统一标识）
        LocalDate mondayOfWeek = targetDate.with(DayOfWeek.MONDAY);
        return mondayOfWeek.format(SystemConstants.WEEK_FORMATTER);
    }

}
