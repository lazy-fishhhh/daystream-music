package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.social.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {
    Long selectLikeCountByUserId(Long userId);
}
