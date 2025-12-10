package com.future.music.mapper;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.SearchUserVO;
import entity.pojo.music.Playlist;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.hibernate.annotations.SqlFragmentAlias;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
//  通过泛型，BaseMapper 能够明确知道当前 Mapper 操作的是哪个实体类
//  从而将数据库表与 Java 实体类关联起来
public interface PlaylistMapper extends BaseMapper<Playlist> {

    // 根据标签筛选歌单（参数来自PlaylistFilterDTO）
    IPage<Playlist> selectPlaylistsByTagIds(
            IPage<Playlist> page,
            @Param("tagIds") List<Long> tagIds,
            @Param("tagSize") Integer tagSize,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("minCollect") Integer minCollect,
            @Param("maxCollect") Integer maxCollect
    );

    // 搜索歌单
    @Select("""
        SELECT DISTINCT p.* 
        FROM playlist p
        -- 关联音乐表（匹配音乐名字）
        LEFT JOIN playlist_music pm ON p.id = pm.playlist_id
        LEFT JOIN music m ON pm.music_id = m.id
        -- 关联标签表
        LEFT JOIN playlist_tag_relation ptr ON p.id = ptr.playlist_id
        LEFT JOIN music_tag mt ON ptr.tag_id = mt.id
        ${ew.customSqlSegment}
        ORDER BY p.play_count DESC
    """)
    IPage<Playlist> searchPlaylistsByMultiCondition(
            Page<Playlist> page,
            @Param("ew") QueryWrapper<Playlist> queryWrapper
    );

    /**
     * 查询收藏了指定targetId（target_type=1、未取消）的用户列表
     * @param targetId 收藏目标ID（参数）
     * @return 符合条件的用户列表
     */
    @Select("SELECT u.id, u.username, u.avatar, u.gender, u.introduction " +
            "FROM `user` u " +
            "INNER JOIN user_collection uc ON u.id = uc.user_id " +
            "WHERE uc.target_type = 1 " +
            "  AND uc.is_canceled = 0 " +
            "  AND u.user_status = 1 " +
            "  AND uc.target_id = #{targetId}")
    List<SearchUserVO> selectUserByCollection(@Param("targetId") Long targetId);

    String selectPlaylistCreator(@Param("playlistId") Long playlistId);

    String selectPlaylistCreatorAvator(Long playlistId);
}

