package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import entity.pojo.music.Musician;
import org.apache.ibatis.annotations.Mapper;



@Mapper
public interface MusicianMapper extends BaseMapper<Musician> {
}
