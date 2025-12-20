package com.future.music.controller;

import com.future.login.util.SecurityUtil;
import com.future.music.service.IUserTaskService;
import com.future.music.service.ITaskConfigService;
import entity.pojo.info.TaskConfig;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.UserTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user/task")
@Api(tags = "用户任务管理")
public class UserTaskController {

    @Autowired
    private IUserTaskService userTaskService;
    
    @Autowired
    private ITaskConfigService taskConfigService;

    @ApiOperation(value = "获取用户任务状态列表（自动补全新增任务）")
    @GetMapping("/list")
    public Result<PageResp<UserTaskVO>> getUserTaskList(PageReq pageReq) {
        Long userId = SecurityUtil.getCurrentUserId();
        // 懒加载补全任务列表 + 获取任务列表（分页）
        PageResp<UserTaskVO> userTasks = userTaskService.getUserTaskListWithLazyLoad(userId, pageReq);
        return Result.ok(userTasks);
    }

    @ApiOperation(value = "领取任务奖励")
    @PostMapping("/reward/claim")
    public Result<String> claimTaskReward(@RequestParam Long taskId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return userTaskService.claimTaskReward(userId, taskId);
    }

    @ApiOperation(value = "添加任务配置")
    @PostMapping("/config/add")
    public Result<Long> addTaskConfig(@RequestBody @Validated TaskConfig taskConfig) {
        return taskConfigService.addTaskConfig(taskConfig);
    }

    @ApiOperation(value = "更新任务配置")
    @PostMapping("/config/update")
    public Result<Boolean> updateTaskConfig(@RequestBody TaskConfig taskConfig) {
        return taskConfigService.updateTaskConfig(taskConfig);
    }

    @ApiOperation(value = "删除任务配置")
    @PostMapping("/config/delete")
    public Result<Boolean> deleteTaskConfig(@RequestParam Long id) {
        return taskConfigService.deleteTaskConfig(id);
    }

}