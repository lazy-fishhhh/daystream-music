package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.MvQualityRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * MV多画质关联表的Mapper接口
 */
@Mapper
public interface MvQualityRelationMapper extends BaseMapper<MvQualityRelation> {
}
