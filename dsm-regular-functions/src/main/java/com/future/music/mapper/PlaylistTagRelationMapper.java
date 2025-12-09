package com.future.music.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.PlaylistTagRelation;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlaylistTagRelationMapper extends BaseMapper<PlaylistTagRelation> {
    /**
     * 批量插入歌单标签关联
     * @param tagRelations 歌单标签关联列表
     * @return 插入的行数
     */
    int insertBatch(@Param("tagRelations") List<PlaylistTagRelation> tagRelations);
}
