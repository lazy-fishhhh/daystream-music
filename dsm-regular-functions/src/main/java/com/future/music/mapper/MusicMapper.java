package com.future.music.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.MusicVO;
import entity.pojo.music.Music;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.List;


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

    /**
     * 根据标签ID列表筛选音乐（包含所有标签）
     * @param page 分页对象（MyBatis-Plus分页）
     * @param tagIds 标签ID列表
     * @param tagSize 标签数量（用于校验是否包含所有标签）
     * @return 分页的音乐列表
     */
    IPage<Music> selectMusicsByTagIds(
            IPage<Music> page,
            @Param("tagIds") List<Long> tagIds,
            @Param("tagSize") Integer tagSize
    );

    /**
     * 前缀匹配歌曲名，按播放量倒序
     */
    @Select("SELECT music_name FROM music WHERE status = 1 AND music_name LIKE #{pattern} ORDER BY play_count DESC LIMIT #{limit}")
    List<String> selectSuggestMusicNames(@Param("pattern") String pattern, @Param("limit") int limit);



    MusicVO selectMusicDetailById(Long musicId);

    /**
     * 批量查询音乐详情
     * @param musicIds 音乐ID列表
     * @return 音乐详情列表
     */
    List<MusicVO> selectMusicDetailByIds(@Param("musicIds") List<Long> musicIds);
}
