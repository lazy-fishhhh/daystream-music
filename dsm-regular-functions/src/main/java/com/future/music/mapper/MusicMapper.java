package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import entity.pojo.music.Music;
import org.mapstruct.Mapper;


@Mapper
public interface MusicMapper extends BaseMapper<Music> {
}
