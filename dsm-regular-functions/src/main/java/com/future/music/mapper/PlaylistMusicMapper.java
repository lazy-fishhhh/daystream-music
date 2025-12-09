package com.future.music.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.MusicVO;
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
    
    /**
     * 分页查询歌单下的音乐VO（连表查询，直接返回VO）
     * @param page 分页参数（页码、每页条数等）
     * @param playlistId 歌单ID
     * @return 分页结果（包含当前页音乐VO列表及总条数）
     */
    @Select("SELECT m.id, m.music_name, m.album_id, m.musician_id, m.cover_url, m.bpm, m.is_vip, " +
            "m.like_count, m.comment_count, m.duration, " +
            "a.album_name, " +
            "mu.stage_name AS musician_name " +
            "FROM playlist_music pm " +
            "INNER JOIN music m ON pm.music_id = m.id " +
            "LEFT JOIN album a ON m.album_id = a.id " +
            "LEFT JOIN musician mu ON m.musician_id = mu.id " +
            "WHERE pm.playlist_id = #{playlistId} " +
            "AND m.status = 1 " +  // 只查询上架的音乐
            "AND (a.id IS NULL OR a.status = 1) " +  // 专辑不存在或已上架
            "AND (mu.id IS NULL OR mu.status = 1) " +  // 音乐人不存在或已认证
            "ORDER BY pm.sort_order ASC")  // 按歌单内排序字段排序
    IPage<MusicVO> selectMusicVOsByPlaylistId(
            Page<MusicVO> page,  // 分页参数（由MyBatis-Plus自动处理分页）
            @Param("playlistId") Long playlistId
    );
}
