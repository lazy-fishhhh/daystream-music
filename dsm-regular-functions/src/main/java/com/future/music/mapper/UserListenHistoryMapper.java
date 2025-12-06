package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.UserListenHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface UserListenHistoryMapper extends BaseMapper<UserListenHistory> {

        /**
         * 插入或更新用户听歌历史记录
         * @param userId 用户ID
         * @param musicId 音乐ID
         * @param playedSeconds 播放时长（秒）
         * @param listenCount 播放次数（达标一次算一次）
         * @param listenTime 听歌日期（按天分组）
         * @return 影响行数
         */
        int insertOrUpdateListenHistory(@Param("userId") Long userId,          // 1. 用户ID
                                        @Param("musicId") Long musicId,        // 2. 歌曲ID
                                        @Param("playedSeconds") Integer playedSeconds,  // 3. 本次播放时长
                                        @Param("listenCount") Integer listenCount,      // 4. 本次有效次数
                                        @Param("listenTime") LocalDate listenTime       // 5. 本次上报时间
        );

}
