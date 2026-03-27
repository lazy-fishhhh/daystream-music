package social.service;

import entity.pojo.social.ChatGroup;
import entity.pojo.social.GroupMessage;
import entity.pojo.social.PrivateMessage;
import social.domain.dto.GroupMessageDTO;
import social.domain.dto.PrivateMessageDTO;
import social.domain.vo.ChatGroupVO;
import social.domain.vo.CreatePrivateSessionResultVO;
import social.domain.vo.OfflineMessageSummaryVO;
import social.domain.vo.PrivateChatUserVO;

import java.util.List;

/**
 * 聊天服务接口
 * 提供私聊和群聊相关的核心业务功能
 */
public interface ChatService {
    
    /**
     * 发送私聊消息
     * @param dto 私聊消息DTO
     */
    void sendPrivateMessage(PrivateMessageDTO dto);
    
    /**
     * 发送群聊消息
     * @param dto 群聊消息DTO
     */
    void sendGroupMessage(GroupMessageDTO dto);
    
    /**
     * 获取用户的私聊离线消息
     * @param userId 用户ID
     * @return 离线消息列表
     */
    List<PrivateMessage> getOfflineMessages(Long userId);
    
    /**
     * 获取用户的群聊离线消息
     * @param userId 用户ID
     * @return 离线消息列表
     */
    List<GroupMessage> getGroupOfflineMessages(Long userId);
    
    /**
     * 标记私聊消息为已读
     * @param msgId 消息ID
     * @param userId 用户ID
     */
    void markPrivateMessageRead(Long msgId, Long userId);
    
    /**
     * 标记群聊消息为已读
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param msgId 消息ID
     */
    void markGroupMessageRead(Long groupId, Long userId, Long msgId);
    
    /**
     * 获取用户加入的所有群聊
     * @param userId 用户ID
     * @return 群聊列表
     */
    List<ChatGroupVO> getUserGroups(Long userId);
    
    /**
     * 获取群聊历史消息
     * @param groupId 群组ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 历史消息列表
     */
    List<GroupMessage> getGroupHistoryMessages(Long groupId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取群聊未读消息条数
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 未读消息条数
     */
    Integer getGroupUnreadCount(Long groupId, Long userId);
    
    /**
     * 群聊未读消息清零（设为已读）
     * @param groupId 群组ID
     * @param userId 用户ID
     */
    void clearGroupUnreadMessages(Long groupId, Long userId);
    
    /**
     * 获取私聊历史消息
     * @param targetUserId 对方用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 历史消息列表
     */
    List<PrivateMessage> getPrivateHistoryMessages(Long targetUserId, Integer pageNum, Integer pageSize);
    
    /**
     * 未读私聊消息清零
     * @param targetUserId 对方用户ID
     * @param userId 当前用户ID
     */
    void clearPrivateUnreadMessages(Long targetUserId, Long userId);
    
    /**
     * 获取用户的私聊对象列表
     * @param userId 用户ID
     * @return 私聊对象列表
     */
    List<PrivateChatUserVO> getPrivateChatUsers(Long userId);
    
    /**
     * 获取用户的离线消息摘要
     * @param userId 用户ID
     * @return 离线消息摘要列表
     */
    List<OfflineMessageSummaryVO> getOfflineMessageSummary(Long userId);
    
    /**
     * 获取用户未读私信数量
     * @param userId 用户ID
     * @return 未读私信数量
     */
    Integer getPrivateMessageUnreadCount(Long userId);
    
    /**
     * 创建私聊会话
     * @param targetUserId 目标用户ID
     * @param userId 当前用户ID
     * @return 创建结果，包含成功状态、会话ID和对方用户信息
     */
    CreatePrivateSessionResultVO createPrivateSession(Long targetUserId, Long userId);
    
    /**
     * 删除私聊会话
     * @param targetUserId 目标用户ID
     * @param userId 当前用户ID
     * @return 删除是否成功
     */
    boolean deletePrivateSession(Long targetUserId, Long userId);
}
