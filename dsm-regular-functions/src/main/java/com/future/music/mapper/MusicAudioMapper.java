package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.entity.vo.GameMusicVO;
import entity.pojo.music.MusicAudio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface MusicAudioMapper extends BaseMapper<MusicAudio> {

    /**
     * 根据固定music_id从music_audio_relation表随机查询一条数据（MySQL版本）
     * @param musicId 固定的歌曲ID
     * @return 随机的一条音频关联数据
     */
    @Select("SELECT * FROM music_audio_relation WHERE music_id = #{musicId} ORDER BY RAND() LIMIT 1")
    MusicAudio selectRandomAudioByMusicId(@Param("musicId") Long musicId);

    /**
     * 查询未使用的随机歌曲
     * @param excludeIds 排除的歌曲ID
     * @return 随机歌曲
     */
    @Select("<script>" +  // 必须添加<script>标签包裹动态SQL
            "SELECT * FROM music_audio_relation " +
            "<where>" +  // 用where标签自动处理多余的AND/OR
            "   <if test='excludeIds != null and excludeIds.size() > 0'>" +  // 空值判断，避免NOT IN ()
            "       music_id NOT IN " +
            "       <foreach collection='excludeIds' item='id' open='(' separator=',' close=')'>" +
            "           #{id}" +
            "       </foreach>" +
            "   </if>" +
            "</where>" +
            "ORDER BY RAND() LIMIT 1" +
            "</script>")
    MusicAudio selectRandomAudio(@Param("excludeIds") Set<Long> excludeIds);
}
