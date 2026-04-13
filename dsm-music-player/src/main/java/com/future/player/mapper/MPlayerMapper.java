package com.future.player.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;


import entity.pojo.music.Music;

import org.mapstruct.Mapper;





@Mapper
public interface MPlayerMapper extends BaseMapper<Music> {
    /**
     * 根据音乐ID查询音频信息
     * @param musicId 音乐ID
     * @return 音频信息列表
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM music_audio_relation WHERE music_id = #{musicId}")
    java.util.List<entity.pojo.music.MusicAudio> selectAudioByMusicId(java.lang.Long musicId);
}
