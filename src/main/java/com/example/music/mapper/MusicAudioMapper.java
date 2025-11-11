package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.entity.pojo.music.MusicAudio;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MusicAudioMapper extends BaseMapper<MusicAudio> {
}
