package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.UserListenHistory;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface UserListenHistoryMapper extends BaseMapper<UserListenHistory> {

        /**
         * 插入或更新用户听歌历史记录
         * @param userId 用户ID
         * @param musicId 音乐ID
         * @param playedSeconds 播放时长（秒）
         * @param listenCount 播放次数（达标一次算一次）
         * @param today 听歌日期（按天分组）
         * @return 影响行数
         */
        int insertOrUpdateListenHistory(Long userId, Long musicId, Integer playedSeconds, Integer listenCount, LocalDate today);

}
