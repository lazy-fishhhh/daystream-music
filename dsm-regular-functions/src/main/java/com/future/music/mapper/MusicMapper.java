package com.future.music.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.music.Music;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mapstruct.Mapper;

import java.math.BigDecimal;


@Mapper
public interface MusicMapper extends BaseMapper<Music> {

    /**
     * 多表联查：按音乐名、标签名、音乐人名字搜索，按播放量倒序
     * @param page 分页参数
     * @param queryWrapper 条件构造器（关键词匹配）
     * @return 分页音乐列表（含关联表匹配逻辑）
     */
    @Select("""
        SELECT DISTINCT m.* 
        FROM music m
        -- 关联音乐人表（匹配音乐人名字）
        LEFT JOIN musician mu ON m.musician_id = mu.id
        -- 关联音乐-标签关联表
        LEFT JOIN music_tag_relation mtr ON m.id = mtr.music_id
        -- 关联标签表（匹配标签名字）
        LEFT JOIN music_tag mt ON mtr.tag_id = mt.id
        ${ew.customSqlSegment}
        ORDER BY m.play_count DESC
    """)
    IPage<Music> searchMusicByMultiCondition(
            Page<Music> page,
            @Param("ew") QueryWrapper<Music> queryWrapper
    );
}
