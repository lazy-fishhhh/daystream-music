package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.util.SecurityUtil;
import entity.pojo.social.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.dto.GroupMessageDTO;
import social.domain.dto.PrivateMessageDTO;
import social.domain.vo.ChatGroupVO;
import social.mapper.*;
import social.service.ChatService;
import social.service.OnlineUserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 * 提供私聊和群聊相关的核心业务功能实现
 * 优化点：批量推送、异步推送、增量广播
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    
    private final PrivateSessionMapper privateSessionMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final SimpMessageSendingOperations messagingTemplate;
    private final OnlineUserService onlineUserService;
    
    /**
     * 发送私聊消息
     * @param dto 私聊消息DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendPrivateMessage(PrivateMessageDTO dto) {
        Long fromUserId = SecurityUtil.getCurrentUserId();
        Long toUserId = dto.getToUser();
        
        Long sessionId = getOrCreatePrivateSession(fromUserId, toUserId);
        
        PrivateMessage message = new PrivateMessage();
        message.setSessionId(sessionId);
        message.setFromUser(fromUserId);
        message.setToUser(toUserId);
        message.setContent(dto.getContent());
        message.setType(dto.getType());
        message.setSendTime(LocalDateTime.now());
        message.setDelivered(0);
        message.setIsRecalled(0);
        
        privateMessageMapper.insert(message);
        
        updatePrivateSession(sessionId, fromUserId, toUserId, dto.getContent(), message.getId());
        
        if (onlineUserService.isUserOnline(toUserId)) {
            asyncSendPrivateMessage(toUserId, message);
            message.setDelivered(1);
            privateMessageMapper.updateById(message);
        }
        
        log.info("私聊消息发送成功，from: {}, to: {}, msgId: {}", fromUserId, toUserId, message.getId());
    }
    
    /**
     * 异步发送私聊消息
     * @param toUserId 接收者用户ID
     * @param message 消息对象
     */
    private void asyncSendPrivateMessage(Long toUserId, PrivateMessage message) {
        OnlineUserService.getMessagePushExecutor().execute(() -> {
            try {
                messagingTemplate.convertAndSendToUser(
                    toUserId.toString(),
                    "/queue/private",
                    message
                );
                log.debug("私聊消息异步推送成功，toUserId: {}, msgId: {}", toUserId, message.getId());
            } catch (Exception e) {
                log.error("私聊消息异步推送失败，toUserId: {}, msgId: {}", toUserId, message.getId(), e);
            }
        });
    }
    
    /**
     * 获取或创建私聊会话
     * @param user1Id 用户1ID
     * @param user2Id 用户2ID
     * @return 会话ID
     */
    private Long getOrCreatePrivateSession(Long user1Id, Long user2Id) {
        Long smallUserId = Math.min(user1Id, user2Id);
        Long largeUserId = Math.max(user1Id, user2Id);
        
        LambdaQueryWrapper<PrivateSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateSession::getUser1Id, smallUserId)
               .eq(PrivateSession::getUser2Id, largeUserId);
        
        PrivateSession session = privateSessionMapper.selectOne(wrapper);
        
        if (session == null) {
            session = new PrivateSession();
            session.setUser1Id(smallUserId);
            session.setUser2Id(largeUserId);
            session.setUnreadCount1(0);
            session.setUnreadCount2(0);
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            privateSessionMapper.insert(session);
        }
        
        return session.getId();
    }
    
    /**
     * 更新私聊会话信息
     * @param sessionId 会话ID
     * @param fromUserId 发送者ID
     * @param toUserId 接收者ID
     * @param content 消息内容
     * @param msgId 消息ID
     */
    private void updatePrivateSession(Long sessionId, Long fromUserId, Long toUserId, 
                                      String content, Long msgId) {
        PrivateSession session = privateSessionMapper.selectById(sessionId);
        session.setLastMessage(content);
        session.setLastMsgTime(LocalDateTime.now());
        session.setLastMsgId(msgId);
        session.setUpdatedAt(LocalDateTime.now());
        
        if (fromUserId.equals(session.getUser1Id())) {
            session.setUnreadCount2(session.getUnreadCount2() + 1);
        } else {
            session.setUnreadCount1(session.getUnreadCount1() + 1);
        }
        
        privateSessionMapper.updateById(session);
    }
    
    /**
     * 发送群聊消息
     * @param dto 群聊消息DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendGroupMessage(GroupMessageDTO dto) {
        Long fromUserId = SecurityUtil.getCurrentUserId();
        Long groupId = dto.getGroupId();
        
        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setFromUser(fromUserId);
        message.setContent(dto.getContent());
        message.setType(dto.getType());
        message.setMentionedUserIds(dto.getMentionedUserIds());
        message.setSendTime(LocalDateTime.now());
        message.setIsRecalled(0);
        
        groupMessageMapper.insert(message);
        
        List<GroupMember> members = groupMemberMapper.selectGroupMembers(groupId);
        Set<Long> onlineUserIds = onlineUserService.getOnlineUsers();
        
        List<Long> onlineMemberIds = members.stream()
            .map(GroupMember::getUserId)
            .filter(userId -> !userId.equals(fromUserId) && onlineUserIds.contains(userId))
            .collect(Collectors.toList());
        
        asyncBatchSendGroupMessages(onlineMemberIds, message);
        
        log.info("群聊消息发送成功，groupId: {}, from: {}, msgId: {}, 推送人数: {}", 
            groupId, fromUserId, message.getId(), onlineMemberIds.size());
    }

    /**
     * 获取用户的私聊离线消息
     * @param userId 用户ID
     * @return 离线消息列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PrivateMessage> getOfflineMessages(Long userId) {
        List<PrivateMessage> messages = privateMessageMapper.selectUndeliveredMessages(userId);
        
        if (!messages.isEmpty()) {
            List<Long> msgIds = messages.stream().map(PrivateMessage::getId).collect(Collectors.toList());
            privateMessageMapper.batchMarkAsDelivered(msgIds);
            log.info("获取私聊离线消息成功，userId: {}, 消息数: {}", userId, messages.size());
        }
        
        return messages;
    }

    /**
     * 获取用户的群聊离线消息
     * @param userId 用户ID
     * @return 离线消息列表
     */
    @Override
    public List<GroupMessage> getGroupOfflineMessages(Long userId) {
        List<GroupMember> userGroups = groupMemberMapper.selectUserGroups(userId);
        
        List<GroupMessage> result = userGroups.stream()
            .flatMap(member -> {
                List<GroupMessage> messages = groupMessageMapper.selectUnreadMessages(
                    member.getGroupId(), 
                    member.getLastReadMsgId()
                );
                return messages.stream();
            })
            .collect(Collectors.toList());
        
        log.info("获取群聊离线消息成功，userId: {}, 消息数: {}", userId, result.size());
        return result;
    }

    /**
     * 标记私聊消息为已读
     * @param msgId 消息ID
     * @param userId 用户ID
     */
    @Override
    public void markPrivateMessageRead(Long msgId, Long userId) {
        int updated = privateMessageMapper.markAsRead(msgId, userId);
        if (updated > 0) {
            log.info("标记私聊消息已读成功，msgId: {}, userId: {}", msgId, userId);
        } else {
            log.warn("标记私聊消息已读失败，msgId: {}, userId: {}", msgId, userId);
        }
    }

    /**
     * 标记群聊消息为已读
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param msgId 消息ID
     */
    @Override
    public void markGroupMessageRead(Long groupId, Long userId, Long msgId) {
        int updated = groupMemberMapper.updateLastReadMsgId(groupId, userId, msgId);
        if (updated > 0) {
            log.info("标记群聊消息已读成功，groupId: {}, userId: {}, msgId: {}", groupId, userId, msgId);
        } else {
            log.warn("标记群聊消息已读失败，groupId: {}, userId: {}, msgId: {}", groupId, userId, msgId);
        }
    }

    /**
     * 批量异步发送群聊消息
     * @param userIds 在线用户ID列表
     * @param message 消息对象
     */
    private void asyncBatchSendGroupMessages(List<Long> userIds, GroupMessage message) {
        if (userIds.isEmpty()) {
            return;
        }
        
        OnlineUserService.getMessagePushExecutor().execute(() -> {
            int successCount = 0;
            int failCount = 0;
            
            for (Long userId : userIds) {
                try {
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/group",
                        message
                    );
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("群聊消息推送失败，userId: {}, msgId: {}", userId, message.getId(), e);
                }
            }
            
            log.debug("群聊消息批量异步推送完成，msgId: {}, 成功: {}, 失败: {}", 
                message.getId(), successCount, failCount);
        });
    }
    
    /**
     * 获取用户加入的所有群聊
     * @param userId 用户ID
     * @return 群聊列表
     */
    @Override
    public List<ChatGroupVO> getUserGroups(Long userId) {
        List<GroupMember> userGroups = groupMemberMapper.selectUserGroups(userId);
        
        if (userGroups.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> groupIds = userGroups.stream()
            .map(GroupMember::getGroupId)
            .collect(Collectors.toList());
        
        List<ChatGroup> chatGroups = chatGroupMapper.selectBatchIds(groupIds);
        
        Map<Long, ChatGroup> groupMap = chatGroups.stream()
            .collect(Collectors.toMap(ChatGroup::getId, group -> group));
        
        List<ChatGroupVO> result = new ArrayList<>();
        for (GroupMember member : userGroups) {
            ChatGroup group = groupMap.get(member.getGroupId());
            if (group != null) {
                ChatGroupVO vo = new ChatGroupVO();
                BeanUtils.copyProperties(group, vo);
                result.add(vo);
            }
        }
        
        return result;
    }
    
    /**
     * 获取群聊历史消息
     * @param groupId 群组ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 历史消息列表
     */
    @Override
    public List<GroupMessage> getGroupHistoryMessages(Long groupId, Integer pageNum, Integer pageSize) {
        Page<GroupMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId)
               .eq(GroupMessage::getIsRecalled, 0)
               .orderByDesc(GroupMessage::getSendTime);
        
        return groupMessageMapper.selectPage(page, wrapper).getRecords();
    }
    
    /**
     * 获取群聊未读消息条数
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 未读消息条数
     */
    @Override
    public Integer getGroupUnreadCount(Long groupId, Long userId) {
        GroupMember member = groupMemberMapper.selectOne(
            new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
        );
        
        if (member == null) {
            return 0;
        }
        
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId)
               .gt(GroupMessage::getId, member.getLastReadMsgId())
               .eq(GroupMessage::getIsRecalled, 0);
        
        return Math.toIntExact(groupMessageMapper.selectCount(wrapper));
    }
    
    /**
     * 群聊未读消息清零（设为已读）
     * @param groupId 群组ID
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearGroupUnreadMessages(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId)
               .eq(GroupMessage::getIsRecalled, 0)
               .orderByDesc(GroupMessage::getId)
               .last("LIMIT 1");
        
        GroupMessage latestMessage = groupMessageMapper.selectOne(wrapper);
        
        if (latestMessage != null) {
            groupMemberMapper.updateLastReadMsgId(groupId, userId, latestMessage.getId());
        }
    }
    
    /**
     * 获取私聊历史消息
     * @param targetUserId 对方用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 历史消息列表
     */
    @Override
    public List<PrivateMessage> getPrivateHistoryMessages(Long targetUserId, Integer pageNum, Integer pageSize) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long smallUserId = Math.min(currentUserId, targetUserId);
        Long largeUserId = Math.max(currentUserId, targetUserId);
        
        PrivateSession session = privateSessionMapper.selectOne(
            new LambdaQueryWrapper<PrivateSession>()
                .eq(PrivateSession::getUser1Id, smallUserId)
                .eq(PrivateSession::getUser2Id, largeUserId)
        );
        
        if (session == null) {
            return new ArrayList<>();
        }
        
        Page<PrivateMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getSessionId, session.getId())
               .eq(PrivateMessage::getIsRecalled, 0)
               .orderByDesc(PrivateMessage::getSendTime);
        
        return privateMessageMapper.selectPage(page, wrapper).getRecords();
    }
    
    /**
     * 未读私聊消息清零
     * @param targetUserId 对方用户ID
     * @param userId 当前用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearPrivateUnreadMessages(Long targetUserId, Long userId) {
        Long smallUserId = Math.min(userId, targetUserId);
        Long largeUserId = Math.max(userId, targetUserId);
        
        PrivateSession session = privateSessionMapper.selectOne(
            new LambdaQueryWrapper<PrivateSession>()
                .eq(PrivateSession::getUser1Id, smallUserId)
                .eq(PrivateSession::getUser2Id, largeUserId)
        );
        
        if (session == null) {
            return;
        }
        
        if (userId.equals(session.getUser1Id())) {
            session.setUnreadCount1(0);
        } else {
            session.setUnreadCount2(0);
        }
        
        session.setUpdatedAt(LocalDateTime.now());
        privateSessionMapper.updateById(session);
    }
}
