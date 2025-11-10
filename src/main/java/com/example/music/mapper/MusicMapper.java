package com.example.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.music.entity.pojo.music.Music;
import org.mapstruct.Mapper;


@Mapper
public interface MusicMapper extends BaseMapper<Music> {
}
