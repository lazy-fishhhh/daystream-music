package com.future.music.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.music.Music;
import entity.pojo.music.PlaylistMusic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlaylistMusicMapper extends BaseMapper<PlaylistMusic> {
    /**
     * 分页查询歌单下的音乐（按歌单内排序）
     * @param page 分页参数（页码、每页条数等）
     * @param playlistId 歌单ID
     * @return 分页结果（包含当前页音乐列表及总条数）
     */
    @Select("SELECT m.* " +
            "FROM playlist_music pm " +
            "INNER JOIN music m ON pm.music_id = m.id " +
            "WHERE pm.playlist_id = #{playlistId} " +
            "AND m.status = 1 " +  // 只查询上架的音乐（可选）
            "ORDER BY pm.sort_order ASC")  // 按歌单内排序字段排序
    IPage<Music> selectMusicsByPlaylistId(
            Page<Music> page,  // 分页参数（由MyBatis-Plus自动处理分页）
            @Param("playlistId") Long playlistId
    );
}
