package social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.social.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.dto.GroupMessageDTO;
import social.domain.dto.PrivateMessageDTO;
import social.mapper.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final PrivateSessionMapper privateSessionMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final SimpMessageSendingOperations messagingTemplate;
    private final OnlineUserService onlineUserService;
    
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

        // 1. 判断【接收消息的用户】当前是否在线（websocket在线状态）
        if (onlineUserService.isUserOnline(toUserId)) {

            // 2. 如果在线 → 立刻通过WebSocket把消息【实时推送给对方】
            messagingTemplate.convertAndSendToUser(
                    toUserId.toString(),   // 推送给谁（接收方ID）
                    "/queue/private",      // 私聊消息的通道
                    message                // 要发送的消息内容
            );

            // 3. 推送成功 → 把消息状态改成【已送达】(1=已送达)
            message.setDelivered(1);

            // 4. 把“已送达”状态更新到数据库
            privateMessageMapper.updateById(message);
        }
        
        log.info("私聊消息发送成功，from: {}, to: {}, msgId: {}", fromUserId, toUserId, message.getId());
    }
    
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
    
    private void updatePrivateSession(Long sessionId, Long fromUserId, Long toUserId, 
                                      String content, Long msgId) {
        PrivateSession session = privateSessionMapper.selectById(sessionId);
        session.setLastMessage(content);
        session.setLastMsgTime(LocalDateTime.now());
        session.setLastMsgId(msgId);
        session.setUpdatedAt(LocalDateTime.now());


        // 核心逻辑：判断是谁发的消息，给【对方】未读数量 + 1;
        if (fromUserId.equals(session.getUser1Id())) {
            // 发送者是 user1 → 给 user2 的未读数 +1
            session.setUnreadCount2(session.getUnreadCount2() + 1);
        } else {
            // 发送者是 user2 → 给 user1 的未读数 +1
            session.setUnreadCount1(session.getUnreadCount1() + 1);
        }
        
        privateSessionMapper.updateById(session);
    }
    
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

        // 遍历【这个群里的所有成员】
        for (GroupMember member : members) {

            // 判断条件：
            // 1. 不是消息发送者本人（跳过自己）
            // 2. 这个群成员当前在线
            if (!member.getUserId().equals(fromUserId) && onlineUserService.isUserOnline(member.getUserId())) {

                // 满足条件 → 给这个群成员【实时推送群消息】
                messagingTemplate.convertAndSendToUser(
                        member.getUserId().toString(),  // 推送给这个群成员
                        "/queue/group",                 // 群消息通道
                        message                         // 发送的群消息
                );
            }
        }
        
        log.info("群聊消息发送成功，groupId: {}, from: {}, msgId: {}", groupId, fromUserId, message.getId());
    }
    
    public List<PrivateMessage> getOfflineMessages(Long userId) {
        List<PrivateMessage> messages = privateMessageMapper.selectUndeliveredMessages(userId);
        
        if (!messages.isEmpty()) {
            List<Long> msgIds = messages.stream().map(PrivateMessage::getId).collect(Collectors.toList());
            privateMessageMapper.batchMarkAsDelivered(msgIds);
        }
        
        return messages;
    }

    /**
     * 获取用户【所有群组】的离线未读消息
     * 作用：用户上线后，拉取他加入的所有群里，未读的群消息
     * @param userId 用户ID
     * @return 该用户所有群的未读群消息列表
     */
    public List<GroupMessage> getGroupOfflineMessages(Long userId) {
        // 1. 查询该用户加入的【所有群组】（获取用户所在的全部群列表）
        List<GroupMember> userGroups = groupMemberMapper.selectUserGroups(userId);

        // 2. 遍历用户所有群，分别拉取每个群的未读消息，最后合并成一个大列表返回
        return userGroups.stream()
                .flatMap(member -> {
                    // 3. 根据【群ID】和【最后已读消息ID】，查询该群里【未读的消息】
                    // 即：比 lastReadMsgId 大的消息都是未读
                    List<GroupMessage> messages = groupMessageMapper.selectUnreadMessages(
                            member.getGroupId(),    // 当前群ID
                            member.getLastReadMsgId() // 用户在这个群最后一次已读的消息ID
                    );
                    // 4. 把每个群的消息流，flatMap 打平合并（最终所有消息在一个列表）
                    return messages.stream();
                })
                // 5. 把所有未读消息收集成 List 返回给前端
                .collect(Collectors.toList());
    }
    
    public void markPrivateMessageRead(Long msgId, Long userId) {
        privateMessageMapper.markAsRead(msgId, userId);
    }
    
    public void markGroupMessageRead(Long groupId, Long userId, Long msgId) {
        groupMemberMapper.updateLastReadMsgId(groupId, userId, msgId);
    }
}
