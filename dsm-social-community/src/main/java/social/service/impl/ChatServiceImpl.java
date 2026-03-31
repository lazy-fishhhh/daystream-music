package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.util.SecurityUtil;
import websocket.ChatEndPoint;
import entity.pojo.social.*;
import entity.pojo.util.WebSocketMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.dto.GroupMessageDTO;
import social.domain.dto.PrivateMessageDTO;
import social.domain.vo.*;
import social.mapper.*;
import social.service.ChatService;
import social.service.NotificationService;
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
public class ChatServiceImpl implements ChatService {
    
    private final PrivateSessionMapper privateSessionMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final User2Mapper user2Mapper;
    private final OnlineUserService onlineUserService;
    private final NotificationService notificationService;



    @Autowired
    public ChatServiceImpl(PrivateSessionMapper privateSessionMapper,
                          PrivateMessageMapper privateMessageMapper,
                          ChatGroupMapper chatGroupMapper,
                          GroupMemberMapper groupMemberMapper,
                          GroupMessageMapper groupMessageMapper,
                          User2Mapper user2Mapper,
                          @Lazy OnlineUserService onlineUserService,
                          NotificationService notificationService) {
        this.privateSessionMapper = privateSessionMapper;
        this.privateMessageMapper = privateMessageMapper;
        this.chatGroupMapper = chatGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.user2Mapper = user2Mapper;
        this.onlineUserService = onlineUserService;
        this.notificationService = notificationService;
    }
    
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
            
            // 计算并推送未读消息数量
            int unreadCount = getPrivateUnreadCount(toUserId, fromUserId);
            asyncPushUnreadCount(toUserId, "private", fromUserId, unreadCount, message.getContent());
            
            // 推送未读通知数量（包括私信数量）
            asyncPushUnreadNotificationCount(toUserId);
        }
        
        log.info("私聊消息发送成功，from: {}, to: {}, msgId: {}", fromUserId, toUserId, message.getId());
    }
    
    /**
     * 异步推送未读通知数量
     * @param userId 用户ID
     */
    private void asyncPushUnreadNotificationCount(Long userId) {
        OnlineUserService.getMessagePushExecutor().execute(() -> {
            try {
                Integer likeUnread = notificationService.getUnreadCountByType(userId, 1);
                Integer commentUnread = notificationService.getUnreadCountByType(userId, 2);
                Integer followUnread = notificationService.getUnreadCountByType(userId, 3);
                Integer privateMessageUnread = getPrivateMessageUnreadCount(userId);
                
                social.domain.vo.UnreadNotificationCountVO unreadCount = new social.domain.vo.UnreadNotificationCountVO(
                    likeUnread, commentUnread, followUnread, privateMessageUnread
                );
                
                // 使用ChatEndPoint发送WebSocket消息
                String message = WebSocketMessageUtils.getMessage(true, null, unreadCount);
                ChatEndPoint.sendMessageToUser(userId, message);
                log.debug("推送未读通知数量成功，userId: {}, 点赞: {}, 评论: {}, 粉丝: {}, 私信: {}", 
                    userId, likeUnread, commentUnread, followUnread, privateMessageUnread);
            } catch (Exception e) {
                log.error("推送未读通知数量失败，userId: {}", userId, e);
            }
        });
    }
    
    /**
     * 异步发送私聊消息
     * @param toUserId 接收者用户ID
     * @param message 消息对象
     */
    private void asyncSendPrivateMessage(Long toUserId, PrivateMessage message) {
        OnlineUserService.getMessagePushExecutor().execute(() -> {
            try {
                // 使用ChatEndPoint发送WebSocket消息
                String wsMessage = WebSocketMessageUtils.getMessage(false, message.getFromUser(), message);
                ChatEndPoint.sendMessageToUser(toUserId, wsMessage);
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
        
        // 为每个在线成员推送未读消息数量
        for (Long userId : onlineMemberIds) {
            int unreadCount = getGroupUnreadCount(groupId, userId);
            asyncPushUnreadCount(userId, "group", groupId, unreadCount, message.getContent());
            
            // 推送未读通知数量（包括私信数量）
            asyncPushUnreadNotificationCount(userId);
        }
        
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
            
            // 推送未读通知数量更新
            asyncPushUnreadNotificationCount(userId);
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
            
            // 推送未读通知数量更新
            asyncPushUnreadNotificationCount(userId);
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
            
            // 使用ChatEndPoint发送WebSocket消息
            String wsMessage = WebSocketMessageUtils.getMessage(false, message.getFromUser(), message);
            ChatEndPoint.sendMessageToUsers(userIds, wsMessage);
            successCount = userIds.size();
            
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
            // 推送未读消息数量更新
            asyncPushUnreadCount(userId, "group", groupId, 0, "");
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
        
        // 推送未读消息数量更新
        asyncPushUnreadCount(userId, "private", targetUserId, 0, "");
    }
    
    /**
     * 获取用户的私聊对象列表
     * @param userId 用户ID
     * @return 私聊对象列表
     */
    @Override
    public List<PrivateChatUserVO> getPrivateChatUsers(Long userId) {
        // 查询用户参与的所有私聊会话
        List<PrivateSession> sessions = privateSessionMapper.selectList(
            new LambdaQueryWrapper<PrivateSession>()
                .and(wrapper -> wrapper
                    .eq(PrivateSession::getUser1Id, userId)
                    .or()
                    .eq(PrivateSession::getUser2Id, userId)
                )
                .orderByDesc(PrivateSession::getUpdatedAt)
        );
        
        List<PrivateChatUserVO> result = new ArrayList<>();
        
        for (PrivateSession session : sessions) {
            // 确定对方用户ID
            Long otherUserId = session.getUser1Id().equals(userId) ? session.getUser2Id() : session.getUser1Id();
            
            // 获取对方用户信息
            OtherUserVO userInfo = user2Mapper.otherUserInfo(otherUserId);
            if (userInfo == null) {
                continue;
            }
            
            // 计算未读消息数量
            int unreadCount = session.getUser1Id().equals(userId) ? session.getUnreadCount1() : session.getUnreadCount2();
            
            // 构造私聊对象VO
            PrivateChatUserVO vo = new PrivateChatUserVO();
            vo.setUserId(otherUserId);
            vo.setNickname(userInfo.getUsername());
            vo.setAvatar(userInfo.getAvatar());
            vo.setLastMessage(session.getLastMessage());
            vo.setLastMessageTime(session.getLastMsgTime());
            vo.setUnreadCount(unreadCount);
            vo.setSessionId(session.getId());
            
            result.add(vo);
        }
        
        log.info("获取用户私聊对象列表成功，userId: {}, 私聊对象数: {}", userId, result.size());
        return result;
    }
    
    /**
     * 获取用户的离线消息摘要
     * @param userId 用户ID
     * @return 离线消息摘要列表
     */
    @Override
    public List<OfflineMessageSummaryVO> getOfflineMessageSummary(Long userId) {
        List<OfflineMessageSummaryVO> result = new ArrayList<>();
        
        // 1. 获取私聊离线消息摘要
        List<PrivateSession> privateSessions = privateSessionMapper.selectList(
            new LambdaQueryWrapper<PrivateSession>()
                .and(wrapper -> wrapper
                    .eq(PrivateSession::getUser1Id, userId)
                    .or()
                    .eq(PrivateSession::getUser2Id, userId)
                )
        );
        
        for (PrivateSession session : privateSessions) {
            // 确定对方用户ID
            Long otherUserId = session.getUser1Id().equals(userId) ? session.getUser2Id() : session.getUser1Id();
            
            // 计算未读消息数量
            int unreadCount = session.getUser1Id().equals(userId) ? session.getUnreadCount1() : session.getUnreadCount2();
            
            // 只有有未读消息时才添加到摘要
            if (unreadCount > 0) {
                // 获取对方用户信息
                OtherUserVO userInfo = user2Mapper.otherUserInfo(otherUserId);
                if (userInfo == null) {
                    continue;
                }
                
                // 构造私聊离线消息摘要
                OfflineMessageSummaryVO vo = new OfflineMessageSummaryVO();
                vo.setType("private");
                vo.setSessionId(otherUserId);
                vo.setOfflineCount(unreadCount);
                vo.setLastMessage(session.getLastMessage());
                vo.setLastMessageTime(session.getLastMsgTime());
                vo.setOtherUserId(otherUserId);
                vo.setOtherUserName(userInfo.getUsername());
                vo.setOtherUserAvatar(userInfo.getAvatar());
                
                result.add(vo);
            }
        }
        
        // 2. 获取群聊离线消息摘要
        List<GroupMember> userGroups = groupMemberMapper.selectUserGroups(userId);
        
        for (GroupMember member : userGroups) {
            Long groupId = member.getGroupId();
            
            // 计算群聊未读消息数量
            int unreadCount = getGroupUnreadCount(groupId, userId);
            
            // 只有有未读消息时才添加到摘要
            if (unreadCount > 0) {
                // 获取群聊信息
                ChatGroup group = chatGroupMapper.selectById(groupId);
                if (group == null) {
                    continue;
                }
                
                // 获取群聊最新一条消息
                GroupMessage latestMessage = groupMessageMapper.selectOne(
                    new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, groupId)
                        .eq(GroupMessage::getIsRecalled, 0)
                        .orderByDesc(GroupMessage::getSendTime)
                        .last("LIMIT 1")
                );
                
                // 构造群聊离线消息摘要
                OfflineMessageSummaryVO vo = new OfflineMessageSummaryVO();
                vo.setType("group");
                vo.setSessionId(groupId);
                vo.setOfflineCount(unreadCount);
                if (latestMessage != null) {
                    vo.setLastMessage(latestMessage.getContent());
                    vo.setLastMessageTime(latestMessage.getSendTime());
                }
                vo.setGroupName(group.getName());
                vo.setGroupAvatar(group.getAvatar());
                
                result.add(vo);
            }
        }
        
        log.info("获取用户离线消息摘要成功，userId: {}, 摘要数: {}", userId, result.size());
        return result;
    }
    
    /**
     * 获取私聊未读消息数量
     * @param userId 当前用户ID
     * @param targetUserId 对方用户ID
     * @return 未读消息数量
     */
    private int getPrivateUnreadCount(Long userId, Long targetUserId) {
        Long smallUserId = Math.min(userId, targetUserId);
        Long largeUserId = Math.max(userId, targetUserId);
        
        PrivateSession session = privateSessionMapper.selectOne(
            new LambdaQueryWrapper<PrivateSession>()
                .eq(PrivateSession::getUser1Id, smallUserId)
                .eq(PrivateSession::getUser2Id, largeUserId)
        );
        
        if (session == null) {
            return 0;
        }
        
        if (userId.equals(session.getUser1Id())) {
            return session.getUnreadCount1();
        } else {
            return session.getUnreadCount2();
        }
    }
    
    /**
     * 异步推送未读消息数量
     * @param userId 接收用户ID
     * @param type 消息类型：private-私聊，group-群聊
     * @param sessionId 会话ID：私聊为对方用户ID，群聊为群ID
     * @param unreadCount 未读消息数量
     * @param lastMessage 最后一条消息内容
     */
    private void asyncPushUnreadCount(Long userId, String type, Long sessionId, int unreadCount, String lastMessage) {
        OnlineUserService.getMessagePushExecutor().execute(() -> {
            try {
                UnreadCountVO vo = new UnreadCountVO();
                vo.setType(type);
                vo.setSessionId(sessionId);
                vo.setUnreadCount(unreadCount);
                vo.setLastMessage(lastMessage);
                vo.setTimestamp(System.currentTimeMillis());
                
                // 使用ChatEndPoint发送WebSocket消息
                String message = WebSocketMessageUtils.getMessage(true, null, vo);
                ChatEndPoint.sendMessageToUser(userId, message);
                log.debug("未读消息数量推送成功，userId: {}, type: {}, sessionId: {}, count: {}", 
                    userId, type, sessionId, unreadCount);
            } catch (Exception e) {
                log.error("未读消息数量推送失败，userId: {}, type: {}, sessionId: {}", 
                    userId, type, sessionId, e);
            }
        });
    }
    
    @Override
    public Integer getPrivateMessageUnreadCount(Long userId) {
        return privateMessageMapper.countUnread(userId);
    }
    
    @Override
    public CreatePrivateSessionResultVO createPrivateSession(Long targetUserId, Long userId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能与自己创建私聊会话");
        }
        
        Long sessionId = getOrCreatePrivateSession(userId, targetUserId);
        
        // 获取对方用户信息
        OtherUserVO otherUser = user2Mapper.otherUserInfo(targetUserId);
        
        return new CreatePrivateSessionResultVO(true, sessionId, otherUser);
    }
    
    @Override
    public boolean deletePrivateSession(Long targetUserId, Long userId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能删除与自己的会话");
        }
        
        Long smallUserId = Math.min(userId, targetUserId);
        Long largeUserId = Math.max(userId, targetUserId);
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrivateSession> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(PrivateSession::getUser1Id, smallUserId)
               .eq(PrivateSession::getUser2Id, largeUserId);
        
        PrivateSession session = privateSessionMapper.selectOne(wrapper);
        if (session != null) {
            // 先删除相关的私信消息
            int delete = privateMessageMapper.delete(
                    new LambdaQueryWrapper<PrivateMessage>()
                            .eq(PrivateMessage::getSessionId, session.getId())
            );

            // 再删除会话
            privateSessionMapper.deleteById(session.getId());
            log.info("删除私聊会话成功，userId: {}, targetUserId: {}, sessionId: {}", userId, targetUserId, session.getId());
            return true;
        }
        
        log.warn("删除私聊会话失败，会话不存在，userId: {}, targetUserId: {}", userId, targetUserId);
        return false;
    }
}
