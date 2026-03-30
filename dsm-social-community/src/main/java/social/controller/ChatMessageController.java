//package social.controller;
//
//import com.future.login.util.SecurityUtil;
//import entity.pojo.social.GroupMessage;
//import entity.pojo.social.PrivateMessage;
//import entity.pojo.util.Result;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import social.domain.dto.GroupMessageDTO;
//import social.domain.dto.PrivateMessageDTO;
//import social.domain.vo.ChatGroupVO;
//import social.domain.vo.CreatePrivateSessionResultVO;
//import social.domain.vo.OfflineMessageSummaryVO;
//import social.domain.vo.PrivateChatUserVO;
//import social.service.ChatService;
//
//import java.util.List;
//
///**
// * 聊天消息控制器
// * 提供聊天相关的REST API接口
// */
//@RestController
//@Api(tags = "聊天消息管理")
//@RequestMapping("/api/chat")
//public class ChatMessageController {
//
//    @Autowired
//    private ChatService chatService;
//
//    /**
//     * 发送私聊消息
//     * @param dto 私聊消息DTO
//     * @return 发送结果
//     */
//    @ApiOperation(value = "发送私聊消息")
//    @PostMapping("/private/send")
//    public Result<Void> sendPrivateMessage(@RequestBody @ApiParam("私聊消息参数") PrivateMessageDTO dto) {
//        chatService.sendPrivateMessage(dto);
//        return Result.ok();
//    }
//
//    /**
//     * 发送群聊消息
//     * @param dto 群聊消息DTO
//     * @return 发送结果
//     */
//    @ApiOperation(value = "发送群聊消息")
//    @PostMapping("/group/send")
//    public Result<Void> sendGroupMessage(@RequestBody @ApiParam("群聊消息参数") GroupMessageDTO dto) {
//        chatService.sendGroupMessage(dto);
//        return Result.ok();
//    }
//
//    /**
//     * 获取用户的私聊离线消息
//     * @return 离线消息列表
//     */
//    @ApiOperation(value = "获取私聊离线消息")
//    @GetMapping("/private/offline")
//    public Result<List<PrivateMessage>> getPrivateOfflineMessages() {
//        Long userId = SecurityUtil.getCurrentUserId();
//        List<PrivateMessage> messages = chatService.getOfflineMessages(userId);
//        return Result.ok(messages);
//    }
//
//    /**
//     * 获取用户的群聊离线消息
//     * @return 离线消息列表
//     */
//    @ApiOperation(value = "获取群聊离线消息")
//    @GetMapping("/group/offline")
//    public Result<List<GroupMessage>> getGroupOfflineMessages() {
//        Long userId = SecurityUtil.getCurrentUserId();
//        List<GroupMessage> messages = chatService.getGroupOfflineMessages(userId);
//        return Result.ok(messages);
//    }
//
//    /**
//     * 标记私聊消息为已读
//     * @param msgId 消息ID
//     * @return 操作结果
//     */
//    @ApiOperation(value = "标记私聊消息为已读")
//    @PutMapping("/private/read/{msgId}")
//    public Result<Void> markPrivateMessageRead(@PathVariable @ApiParam("消息ID") Long msgId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        chatService.markPrivateMessageRead(msgId, userId);
//        return Result.ok();
//    }
//
//    /**
//     * 标记群聊消息为已读
//     * @param groupId 群组ID
//     * @param msgId 消息ID
//     * @return 操作结果
//     */
//    @ApiOperation(value = "标记群聊消息为已读")
//    @PutMapping("/group/read/{groupId}/{msgId}")
//    public Result<Void> markGroupMessageRead(
//            @PathVariable @ApiParam("群组ID") Long groupId,
//            @PathVariable @ApiParam("消息ID") Long msgId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        chatService.markGroupMessageRead(groupId, userId, msgId);
//        return Result.ok();
//    }
//
//    /**
//     * 获取用户加入的所有群聊
//     * @return 群聊列表
//     */
//    @ApiOperation(value = "获取用户加入的所有群聊")
//    @GetMapping("/groups")
//    public Result<List<ChatGroupVO>> getUserGroups() {
//        Long userId = SecurityUtil.getCurrentUserId();
//        List<ChatGroupVO> groups = chatService.getUserGroups(userId);
//        return Result.ok(groups);
//    }
//
//    /**
//     * 获取群聊历史消息
//     * @param groupId 群组ID
//     * @param pageNum 页码
//     * @param pageSize 每页大小
//     * @return 历史消息列表
//     */
//    @ApiOperation(value = "获取群聊历史消息")
//    @GetMapping("/group/history/{groupId}")
//    public Result<List<GroupMessage>> getGroupHistoryMessages(
//            @PathVariable @ApiParam("群组ID") Long groupId,
//            @RequestParam(defaultValue = "1") @ApiParam("页码") Integer pageNum,
//            @RequestParam(defaultValue = "20") @ApiParam("每页大小") Integer pageSize) {
//        List<GroupMessage> messages = chatService.getGroupHistoryMessages(groupId, pageNum, pageSize);
//        return Result.ok(messages);
//    }
//
//    /**
//     * 获取群聊未读消息条数
//     * @param groupId 群组ID
//     * @return 未读消息条数
//     */
//    @ApiOperation(value = "获取群聊未读消息条数")
//    @GetMapping("/group/unread/{groupId}")
//    public Result<Integer> getGroupUnreadCount(@PathVariable @ApiParam("群组ID") Long groupId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        Integer unreadCount = chatService.getGroupUnreadCount(groupId, userId);
//        return Result.ok(unreadCount);
//    }
//
//    /**
//     * 群聊未读消息清零（设为已读）
//     * @param groupId 群组ID
//     * @return 操作结果
//     */
//    @ApiOperation(value = "群聊未读消息清零")
//    @PutMapping("/group/unread/clear/{groupId}")
//    public Result<Void> clearGroupUnreadMessages(@PathVariable @ApiParam("群组ID") Long groupId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        chatService.clearGroupUnreadMessages(groupId, userId);
//        return Result.ok();
//    }
//
//    /**
//     * 获取私聊历史消息
//     * @param targetUserId 对方用户ID
//     * @param pageNum 页码
//     * @param pageSize 每页大小
//     * @return 历史消息列表
//     */
//    @ApiOperation(value = "获取私聊历史消息")
//    @GetMapping("/private/history/{targetUserId}")
//    public Result<List<PrivateMessage>> getPrivateHistoryMessages(
//            @PathVariable @ApiParam("对方用户ID") Long targetUserId,
//            @RequestParam(defaultValue = "1") @ApiParam("页码") Integer pageNum,
//            @RequestParam(defaultValue = "20") @ApiParam("每页大小") Integer pageSize) {
//        List<PrivateMessage> messages = chatService.getPrivateHistoryMessages(targetUserId, pageNum, pageSize);
//        return Result.ok(messages);
//    }
//
//    /**
//     * 未读私聊消息清零
//     * @param targetUserId 对方用户ID
//     * @return 操作结果
//     */
//    @ApiOperation(value = "未读私聊消息清零")
//    @PutMapping("/private/unread/clear/{targetUserId}")
//    public Result<Void> clearPrivateUnreadMessages(@PathVariable @ApiParam("对方用户ID") Long targetUserId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        chatService.clearPrivateUnreadMessages(targetUserId, userId);
//        return Result.ok();
//    }
//
//    /**
//     * 获取用户的私聊对象列表
//     * @return 私聊对象列表
//     */
//    @ApiOperation(value = "获取用户的私聊对象列表")
//    @GetMapping("/private/users")
//    public Result<List<PrivateChatUserVO>> getPrivateChatUsers() {
//        Long userId = SecurityUtil.getCurrentUserId();
//        List<PrivateChatUserVO> users = chatService.getPrivateChatUsers(userId);
//        return Result.ok(users);
//    }
//
//    /**
//     * 获取用户的离线消息摘要
//     * @return 离线消息摘要列表
//     */
//    @ApiOperation(value = "获取用户的离线消息摘要")
//    @GetMapping("/offline/summary")
//    public Result<List<OfflineMessageSummaryVO>> getOfflineMessageSummary() {
//        Long userId = SecurityUtil.getCurrentUserId();
//        List<OfflineMessageSummaryVO> summary = chatService.getOfflineMessageSummary(userId);
//        return Result.ok(summary);
//    }
//
//    /**
//     * 创建私聊会话
//     * @param targetUserId 目标用户ID
//     * @return 创建结果，包含成功状态、会话ID和对方用户信息
//     */
//    @ApiOperation(value = "创建私聊会话")
//    @PostMapping("/private/session/create/{targetUserId}")
//    public Result<CreatePrivateSessionResultVO> createPrivateSession(@PathVariable @ApiParam("目标用户ID") Long targetUserId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        CreatePrivateSessionResultVO result = chatService.createPrivateSession(targetUserId, userId);
//        return Result.ok(result);
//    }
//
//    /**
//     * 删除私聊会话
//     * @param targetUserId 目标用户ID
//     * @return 操作结果
//     */
//    @ApiOperation(value = "删除私聊会话")
//    @DeleteMapping("/private/session/delete/{targetUserId}")
//    public Result<Boolean> deletePrivateSession(@PathVariable @ApiParam("目标用户ID") Long targetUserId) {
//        Long userId = SecurityUtil.getCurrentUserId();
//        boolean success = chatService.deletePrivateSession(targetUserId, userId);
//        return Result.ok(success);
//    }
//}
