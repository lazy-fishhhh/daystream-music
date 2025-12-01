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
     * 按标签类型查询标签列表
     * @param tagType 1=流派 2=心情 3=场景 4=乐器
     * @return 对应类型标签列表
     */
    @Select("SELECT id, tag_name as tagName, tag_type as tagType FROM music_tag WHERE tag_type = #{tagType} ORDER BY id ASC")
    List<MusicTag> selectTagsByType(Integer tagType);


    /**
     * 根据音乐ID查询对应的所有标签名
     * @param musicId 音乐ID
     * @return 标签名集合
     */
    @Select("SELECT mt.tag_name " +
            "FROM music_tag mt " +
            "JOIN music_tag_relation mtr ON mt.id = mtr.tag_id " +
            "WHERE mtr.music_id = #{musicId}")
    List<String> getTagNamesByMusicId(@Param("musicId") Long musicId);

    /**
     * 前缀匹配标签名，按关联歌曲数倒序（热门优先）
     * 注：music_tag表需新增关联歌曲数字段music_count，或通过子查询统计
     */
    @Select("""
        SELECT tag_name FROM music_tag 
        WHERE tag_name LIKE #{pattern} 
        LIMIT #{limit}
    """)
    List<String> selectSuggestTagNames(@Param("pattern") String pattern, @Param("limit") int limit);

}
