package com.future.ai.service;

import com.future.ai.domain.entity.AiChatMessage;
import entity.pojo.util.Result;

import java.util.List;

public interface MessageService {

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
