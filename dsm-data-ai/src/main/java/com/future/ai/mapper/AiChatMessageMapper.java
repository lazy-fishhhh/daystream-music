package com.future.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.ai.domain.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
