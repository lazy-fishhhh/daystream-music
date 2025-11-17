package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.MusicTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MusicTagMapper extends BaseMapper<MusicTag> {
    /**
     * 根据音乐ID查询对应的所有标签名
     * @param musicId 音乐ID
     * @return 标签名集合
     */
    @Select("SELECT mt.name " +
            "FROM music_tag mt " +
            "JOIN music_tag_relation mtr ON mt.id = mtr.tag_id " +
            "WHERE mtr.music_id = #{musicId}")
    List<String> getTagNamesByMusicId(@Param("musicId") Long musicId);
}
