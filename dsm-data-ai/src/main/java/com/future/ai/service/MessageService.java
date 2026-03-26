package com.future.ai.service;

import com.future.ai.domain.entity.AiChatMessage;
import com.future.ai.domain.entity.AiChatSession;
import entity.pojo.util.Result;

import java.util.List;

public interface MessageService {

     /**
      * 查询用户会话列表
      * @return 会话列表
      */
    Result<List<AiChatSession>> getSessions(String sessionType, Integer pageNum, Integer pageSize);

     /**
      * 根据会话id查询消息记录
      * @param memoryId 消息ID
      * @return 消息内容
      */
    Result<List<AiChatMessage>> getMessage(String memoryId, Integer pageNum, Integer pageSize);

     /**
      * 删除会话
      * @param memoryId 会话ID
      */
    Result<Void> deleteMessages(String memoryId);


}
