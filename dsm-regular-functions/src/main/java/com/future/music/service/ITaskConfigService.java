package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import entity.pojo.info.TaskConfig;
import entity.pojo.util.Result;

import java.util.List;

/**
 * 任务配置服务接口
 *
 * @author future
 * @date 2023-09-01
 */
public interface ITaskConfigService extends IService<TaskConfig> {
    /**
     * 添加任务配置
     * @param taskConfig 任务配置信息
     * @return 操作结果
     */
    Result<Long> addTaskConfig(TaskConfig taskConfig);

    /**
     * 更新任务配置
     * @param taskConfig 任务配置信息
     * @return 操作结果
     */
    Result<Boolean> updateTaskConfig(TaskConfig taskConfig);

    /**
     * 删除任务配置
     * @param id 任务配置ID
     * @return 操作结果
     */
    Result<Boolean> deleteTaskConfig(Long id);

    /**
     * 获取任务配置详情
     * @param id 任务配置ID
     * @return 任务配置详情
     */
    Result<TaskConfig> getTaskConfigById(Long id);
    
    /**
     * 获取所有任务配置列表
     * @return 任务配置列表
     */
    Result<List<TaskConfig>> getAllTaskConfigs();
}
