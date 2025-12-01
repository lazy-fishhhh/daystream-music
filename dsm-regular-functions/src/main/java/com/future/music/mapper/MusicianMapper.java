package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import entity.pojo.music.Musician;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface MusicianMapper extends BaseMapper<Musician> {

    /**
     * 前缀匹配音乐人名字，按关联歌曲数倒序（热门优先）
     * 注：musician表需新增关联歌曲数字段music_count，或通过子查询统计
     */
    @Select("""
        SELECT stage_name FROM musician 
        WHERE status = 1 AND stage_name LIKE #{pattern} 
        ORDER BY total_fans DESC LIMIT #{limit}
    """)
    List<String> selectSuggestMusicianNames(@Param("pattern") String pattern, @Param("limit") int limit);
}
