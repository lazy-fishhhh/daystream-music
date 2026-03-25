package social.controller;

import com.future.login.util.SecurityUtil;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import social.domain.vo.NotificationVO;
import social.domain.vo.UnreadNotificationCountVO;
import social.service.NotificationService;
import social.service.OnlineUserService;

import java.util.List;

/**
 * 通知控制器
 * 处理通知相关的API请求
 */
@RestController
@RequestMapping("/api/notification")
@Api(tags = "通知管理")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final OnlineUserService onlineUserService;
    
    @Autowired
    public NotificationController(NotificationService notificationService, OnlineUserService onlineUserService) {
        this.notificationService = notificationService;
        this.onlineUserService = onlineUserService;
    }
    
    /**
     * 获取用户的通知列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 通知列表
     */
    @ApiOperation(value = "获取所有通知列表")
    @GetMapping("/list")
    public Result<List<NotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") @ApiParam("页码") Integer pageNum,
            @RequestParam(defaultValue = "20") @ApiParam("每页大小") Integer pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotificationVO> notifications = notificationService.getNotifications(userId, pageNum, pageSize);
        return Result.ok(notifications);
    }
    
    /**
     * 获取用户点赞通知列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 点赞通知列表
     */
    @ApiOperation(value = "获取点赞通知列表")
    @GetMapping("/list/like")
    public Result<List<NotificationVO>> getLikeNotifications(
            @RequestParam(defaultValue = "1") @ApiParam("页码") Integer pageNum,
            @RequestParam(defaultValue = "20") @ApiParam("每页大小") Integer pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotificationVO> notifications = notificationService.getLikeNotifications(userId, pageNum, pageSize);
        return Result.ok(notifications);
    }
    
    /**
     * 获取用户评论通知列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 评论通知列表
     */
    @ApiOperation(value = "获取评论通知列表")
    @GetMapping("/list/comment")
    public Result<List<NotificationVO>> getCommentNotifications(
            @RequestParam(defaultValue = "1") @ApiParam("页码") Integer pageNum,
            @RequestParam(defaultValue = "20") @ApiParam("每页大小") Integer pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotificationVO> notifications = notificationService.getCommentNotifications(userId, pageNum, pageSize);
        return Result.ok(notifications);
    }
    
    /**
     * 获取用户粉丝通知列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 粉丝通知列表
     */
    @ApiOperation(value = "获取粉丝通知列表")
    @GetMapping("/list/follow")
    public Result<List<NotificationVO>> getFollowNotifications(
            @RequestParam(defaultValue = "1") @ApiParam("页码") Integer pageNum,
            @RequestParam(defaultValue = "20") @ApiParam("每页大小") Integer pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotificationVO> notifications = notificationService.getFollowNotifications(userId, pageNum, pageSize);
        return Result.ok(notifications);
    }
    
    /**
     * 获取用户未读消息数量（按类型）
     * @param type 通知类型：1-点赞，2-评论，3-粉丝
     * @return 未读消息数量
     */
    @ApiOperation(value = "获取未读消息数量（按类型）")
    @GetMapping("/unread/count/{type}")
    public Result<Integer> getUnreadCountByType(@PathVariable @ApiParam("通知类型") Integer type) {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer count = notificationService.getUnreadCountByType(userId, type);
        return Result.ok(count);
    }
    
    /**
     * 获取用户所有未读消息数量
     * @return 未读消息数量（点赞、评论、粉丝、私信）
     */
    @ApiOperation(value = "获取所有未读消息数量")
    @GetMapping("/unread/count/all")
    public Result<UnreadNotificationCountVO> getAllUnreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        Integer likeCount = notificationService.getUnreadCountByType(userId, 1);
        Integer commentCount = notificationService.getUnreadCountByType(userId, 2);
        Integer followCount = notificationService.getUnreadCountByType(userId, 3);
        Integer privateMessageCount = onlineUserService.getPrivateMessageUnreadCount(userId);
        UnreadNotificationCountVO count = new UnreadNotificationCountVO(likeCount, commentCount, followCount, privateMessageCount);
        return Result.ok(count);
    }
    
    /**
     * 标记通知为已读
     * @param id 通知ID
     * @return 操作结果
     */
    @ApiOperation(value = "标记通知为已读")
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable @ApiParam("通知ID") Long id) {
        notificationService.markAsRead(id);
        return Result.ok();
    }
    
    /**
     * 批量标记通知为已读
     * @param ids 通知ID列表
     * @return 操作结果
     */
    @ApiOperation(value = "批量标记通知为已读")
    @PutMapping("/read/batch")
    public Result<Void> markBatchAsRead(@RequestBody @ApiParam("通知ID列表") List<Long> ids) {
        notificationService.markBatchAsRead(ids);
        return Result.ok();
    }
    
    /**
     * 标记所有通知为已读
     * @return 操作结果
     */
    @ApiOperation(value = "标记所有通知为已读")
    @PutMapping("/read/all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
    
    /**
     * 获取用户的未读通知
     * @return 未读通知列表
     */
    @ApiOperation(value = "获取未读通知")
    @GetMapping("/unread")
    public Result<List<NotificationVO>> getUnreadNotifications() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<NotificationVO> notifications = notificationService.getUnreadNotifications(userId);
        return Result.ok(notifications);
    }
}
