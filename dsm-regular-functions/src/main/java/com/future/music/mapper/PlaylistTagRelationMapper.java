package com.future.music.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.PlaylistTagRelation;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlaylistTagRelationMapper extends BaseMapper<PlaylistTagRelation> {
}
