package com.future.music.mapper;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.music.Playlist;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
//  通过泛型，BaseMapper 能够明确知道当前 Mapper 操作的是哪个实体类
//  从而将数据库表与 Java 实体类关联起来
public interface PlayListMapper extends BaseMapper<Playlist> {
    void selectPlayList(Page page, LambdaQueryWrapper<Playlist> lqw);

    @Select("""
    SELECT p.* 
    FROM playlist p
    INNER JOIN playlist_tag_relation ptr ON p.id = ptr.playlist_id
    WHERE ptr.tag_id IN 
    <foreach collection="tagIds" item="tagId" open="(" separator="," close=")">
        #{tagId}
    </foreach>
    AND p.is_public = 1
    GROUP BY p.id
    HAVING COUNT(DISTINCT ptr.tag_id) = #{tagIds.size}
    ORDER BY p.play_count DESC
    """)
    IPage<Playlist> selectPlaylistsByTagIds(
            IPage<Playlist> page,
            @Param("tagIds") List<Long> tagIds
    );
}
