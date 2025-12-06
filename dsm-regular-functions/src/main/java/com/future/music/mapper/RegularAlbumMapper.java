package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.domain.vo.AlbumVO;
import com.future.music.domain.vo.MusicVO;
import entity.pojo.music.Album;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegularAlbumMapper extends BaseMapper<Album> {

    /**
     * 根据专辑ID查询专辑详情
     * @param albumId 专辑ID
     * @return 专辑详情VO
     */
    @Select("SELECT id, album_name as albumName, musician_id as musicianId, cover_url as coverUrl, release_date as releaseDate, introduction, status, collect_count as collectCount, comment_count as commentCount, music_count as musicCount FROM album WHERE id = #{albumId}")
    AlbumVO selectAlbumDetailById(@Param("albumId") Long albumId);

    /**
     * 根据专辑ID查询所有歌曲
     * @param albumId 专辑ID
     * @return 歌曲列表VO
     */
    @Select("SELECT m.id, m.music_name as musicName, m.musician_id as musicianId, m.duration, m.cover_url as coverUrl, m.album_id as albumId, m.play_count as playCount, m.status FROM music m WHERE m.album_id = #{albumId} AND m.status = 1 ORDER BY m.id ASC")
    List<MusicVO> selectMusicsByAlbumId(@Param("albumId") Long albumId);
}