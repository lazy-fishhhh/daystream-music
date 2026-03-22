package social.controller;

import com.future.login.util.SecurityUtil;
import entity.pojo.social.GroupMessage;
import entity.pojo.social.PrivateMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import social.domain.dto.GroupMessageDTO;
import social.domain.dto.PrivateMessageDTO;
import social.service.ChatService;

import java.util.List;

@Api(tags = "聊天接口")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatMessageController {
    
    private final ChatService chatService;
    
    @MessageMapping("/private/send") //专门用来接收前端通过 WebSocket 发来的消息
    @ApiOperation("发送私聊消息")
    public void sendPrivateMessage(@Payload PrivateMessageDTO dto) {
        chatService.sendPrivateMessage(dto);
    }
    
    @MessageMapping("/group/send")
    @ApiOperation("发送群聊消息")
    public void sendGroupMessage(@Payload GroupMessageDTO dto) {
        chatService.sendGroupMessage(dto);
    }
    
    @GetMapping("/private/offline")
    @ApiOperation("获取私聊离线消息")
    public List<PrivateMessage> getPrivateOfflineMessages() {
        Long userId = SecurityUtil.getCurrentUserId();
        return chatService.getOfflineMessages(userId);
    }
    
    @GetMapping("/group/offline")
    @ApiOperation("获取群聊离线消息")
    public List<GroupMessage> getGroupOfflineMessages() {
        Long userId = SecurityUtil.getCurrentUserId();
        return chatService.getGroupOfflineMessages(userId);
    }
    
    @PutMapping("/private/{msgId}/read")
    @ApiOperation("标记私聊消息已读")
    public void markPrivateMessageRead(@PathVariable Long msgId) {
        Long userId = SecurityUtil.getCurrentUserId();
        chatService.markPrivateMessageRead(msgId, userId);
    }
    
    @PutMapping("/group/{groupId}/read")
    @ApiOperation("标记群聊消息已读")
    public void markGroupMessageRead(@PathVariable Long groupId, @RequestParam Long msgId) {
        Long userId = SecurityUtil.getCurrentUserId();
        chatService.markGroupMessageRead(groupId, userId, msgId);
    }
}
