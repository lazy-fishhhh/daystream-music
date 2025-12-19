package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserMapper;
import com.future.music.mapper.TaskConfigMapper;
import com.future.music.mapper.UserTaskMapper;
import com.future.music.service.IUserTaskService;
import entity.pojo.info.TaskConfig;
import entity.pojo.info.User;
import entity.pojo.info.UserTask;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.UserTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UserTaskServiceImpl extends ServiceImpl<UserTaskMapper, UserTask> implements IUserTaskService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TaskConfigMapper taskConfigMapper;

    @Autowired
    private UserTaskMapper userTaskMapper;

    // ========== 核心修改1：规范Redis键命名（避免冲突） ==========
    private static final String REDIS_KEY_PREFIX = "music:task:";
    private static final String USER_COMPLETED_TASKS_SET = REDIS_KEY_PREFIX + "completed:%s"; // %s=userId
    private static final String USER_TASK_PROGRESS = REDIS_KEY_PREFIX + "progress:%s:%s:%s"; // %s=userId,taskId,date
    private static final String USER_TASK_CLAIMED_SET = REDIS_KEY_PREFIX + "claimed:%s"; // %s=userId

    // ========== 核心修改2：修正任务类型常量（与业务匹配） ==========
    /** 任务类型：1=一次性任务（完善信息/绑手机号） */
    public static final Integer TASK_TYPE_ONETIME = 1;
    /** 任务类型：2=计数/时长任务（每日听歌30分钟） */
    public static final Integer TASK_TYPE_COUNT = 2;
    /** 任务类型：3=操作类任务（创建歌单/兑换会员） */
    public static final Integer TASK_TYPE_ACTION = 3;
    /** 任务类型：4=每日重复任务（每日听歌） */
    public static final Integer TASK_TYPE_DAILY = 4;

    // 任务状态常量（保留，补充注释）
    /** 未完成 */
    public static final Integer TASK_STATUS_NOT_COMPLETED = 0;
    /** 已完成 */
    public static final Integer TASK_STATUS_COMPLETED = 1;
    /** 已领奖 */
    public static final Integer TASK_STATUS_REWARDED = 2;
    @Autowired
    private UserMapper userMapper;







    // ========== 优化用户任务初始化（批量插入，避免循环） ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserTasks(Long userId) {
        // 1. 查询所有启用的任务
        List<TaskConfig> taskConfigs = taskConfigMapper.selectList(
                new QueryWrapper<TaskConfig>().eq("status", 1)
        );
        if (CollectionUtils.isEmpty(taskConfigs)) {
            return;
        }

        // 2. 查询用户已有的任务ID
        List<UserTask> existUserTasks = userTaskMapper.selectList(
                new QueryWrapper<UserTask>().eq("user_id", userId)
        );
        Set<Long> existTaskIds = existUserTasks.stream()
                .map(UserTask::getTaskId)
                .collect(Collectors.toSet());

        // 3. 批量组装缺失的任务记录
        List<UserTask> newUserTasks = new ArrayList<>();
        for (TaskConfig taskConfig : taskConfigs) {
            if (!existTaskIds.contains(taskConfig.getId())) {
                UserTask userTask = new UserTask();
                userTask.setUserId(userId);
                userTask.setTaskId(taskConfig.getId());
                userTask.setTaskStatus(TASK_STATUS_NOT_COMPLETED);
                userTask.setCreateTime(LocalDateTime.now());
                userTask.setUpdateTime(LocalDateTime.now());
                newUserTasks.add(userTask);
            }
        }

        // 4. 批量插入（性能优化）
        if (!CollectionUtils.isEmpty(newUserTasks)) {
            userTaskMapper.batchInsert(newUserTasks);
        }
    }

    // ==========懒加载补全的任务列表方法==========
    @Override
    public PageResp<UserTaskVO> getUserTaskListWithLazyLoad(Long userId, PageReq pageReq) {
        // 1. 先初始化缺失的任务
        initUserTasks(userId);

        // 2. 构建分页参数
        Page<UserTaskVO> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());

        // 3. 使用连表分页查询获取用户任务列表及对应的任务配置信息
        userTaskMapper.selectUserTaskVOByUserIdWithPage(userId, page);

        // 4. 转换为通用分页响应对象
        PageResp<UserTaskVO> pageResp = new PageResp<>();
        pageResp.setRecords(page.getRecords());
        pageResp.setTotal(page.getTotal());
        pageResp.setSize(page.getSize());
        pageResp.setCurrent(page.getCurrent());
        pageResp.setPages(page.getPages());

        return pageResp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> claimTaskReward(Long userId, Long taskId) {

        // 2. 校验任务状态
        UserTask userTask = userTaskMapper.selectByUserIdAndTaskId(userId, taskId);
        if (userTask == null || Objects.equals(userTask.getTaskStatus(), TASK_STATUS_NOT_COMPLETED)) {
            return Result.ok("任务未完成，无法领取奖励");
        }

        if (Objects.equals(userTask.getTaskStatus(), TASK_STATUS_REWARDED)) {
            return Result.ok("任务已领取过奖励");
        }

        // 3. 更新任务状态为已领奖
        userTask.setTaskStatus(TASK_STATUS_REWARDED);
        userTask.setRewardGetTime(LocalDateTime.now());
        userTask.setUpdateTime(LocalDateTime.now());
        userTaskMapper.updateById(userTask);

        // 4. 发放奖励
        TaskConfig taskConfig = taskConfigMapper.selectById(taskId);
        if (taskConfig == null) {
            throw new BusinessException("任务配置不存在");
        }

        if (taskConfig.getRewardType() == 1) {
            // 音浪奖励
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, userId)
                    .setSql("wallet_balance = wallet_balance + " + taskConfig.getRewardValue());

            int updateCount = userMapper.update(null, updateWrapper);
            if (updateCount <= 0) {
                throw new BusinessException("奖励发放失败，请稍后重试");
            }

            log.info("用户音浪奖励发放成功，userId:{}, 增加金额:{}", userId, taskConfig.getRewardValue());
        }
//        else if (taskConfig.getRewardType() == 2){
//
//        }else if (taskConfig.getRewardType() == 3){
//
//        }
        else {
            log.warn("用户任务奖励发放失败，任务ID:{}, 奖励类型未知", taskId);
            throw new BusinessException("任务奖励类型未知");
        }

        return Result.ok("任务奖励领取成功");
    }

}