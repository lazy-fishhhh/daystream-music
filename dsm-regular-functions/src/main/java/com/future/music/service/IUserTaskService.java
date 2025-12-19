package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import entity.pojo.info.UserTask;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.UserTaskVO;

import java.time.LocalDate;
import java.util.List;

public interface IUserTaskService extends IService<UserTask> {


    void initUserTasks(Long userId);

    Result<String> claimTaskReward(Long userId, Long taskId);

    // 懒加载补全的任务列表方法
    PageResp<UserTaskVO> getUserTaskListWithLazyLoad(Long userId, PageReq pageReq);

}