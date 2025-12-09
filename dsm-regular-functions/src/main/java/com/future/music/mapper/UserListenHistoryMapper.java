package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.domain.dto.DailyListenCountDTO;
import entity.pojo.music.UserListenHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * 查询用户本周每日的听歌次数
     * @param userId 用户ID
     * @param startDate 本周开始日期
     * @param endDate 本周结束日期
     * @return 每日听歌次数的集合
     */
    @Select("SELECT DATE(listen_time) as day, COALESCE(SUM(listen_count), 0) as count FROM user_listen_history WHERE user_id = #{userId} AND listen_time BETWEEN #{startDate} AND #{endDate} GROUP BY DATE(listen_time) ORDER BY day")
    List<DailyListenCountDTO> getWeeklyDailyListenCount(@Param("userId") Long userId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);


}
