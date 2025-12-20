package com.future.music.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.mapper.TaskConfigMapper;
import com.future.music.service.ITaskConfigService;
import entity.pojo.info.TaskConfig;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务配置服务实现类
 *
 * @author future
 * @date 2023-09-01
 */
@Service
@Slf4j
public class TaskConfigServiceImpl extends ServiceImpl<TaskConfigMapper, TaskConfig> implements ITaskConfigService {

    @Override
    public Result<Long> addTaskConfig(TaskConfig taskConfig) {
        try {
            // 设置创建时间和更新时间
            taskConfig.setCreateTime(LocalDateTime.now());
            taskConfig.setUpdateTime(LocalDateTime.now());
            
            // 保存任务配置
            boolean saveResult = save(taskConfig);
            if (saveResult) {
                log.info("添加任务配置成功，ID: {}", taskConfig.getId());
                return Result.ok(taskConfig.getId());
            } else {
                log.error("添加任务配置失败");
                return Result.fail("添加任务配置失败");
            }
        } catch (Exception e) {
            log.error("添加任务配置异常: {}", e.getMessage(), e);
            return Result.fail("添加任务配置异常: " + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> updateTaskConfig(TaskConfig taskConfig) {
        try {
            // 设置更新时间
            taskConfig.setUpdateTime(LocalDateTime.now());
            
            // 更新任务配置
            boolean updateResult = updateById(taskConfig);
            if (updateResult) {
                log.info("更新任务配置成功，ID: {}", taskConfig.getId());
                return Result.ok(true);
            } else {
                log.error("更新任务配置失败，ID: {}", taskConfig.getId());
                return Result.fail("更新任务配置失败");
            }
        } catch (Exception e) {
            log.error("更新任务配置异常: {}", e.getMessage(), e);
            return Result.fail("更新任务配置异常: " + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> deleteTaskConfig(Long id) {
        try {
            // 删除任务配置
            boolean deleteResult = removeById(id);
            if (deleteResult) {
                log.info("删除任务配置成功，ID: {}", id);
                return Result.ok(true);
            } else {
                log.error("删除任务配置失败，ID: {}", id);
                return Result.fail("删除任务配置失败");
            }
        } catch (Exception e) {
            log.error("删除任务配置异常: {}", e.getMessage(), e);
            return Result.fail("删除任务配置异常: " + e.getMessage());
        }
    }

    @Override
    public Result<TaskConfig> getTaskConfigById(Long id) {
        try {
            // 获取任务配置详情
            TaskConfig taskConfig = getById(id);
            if (taskConfig != null) {
                log.info("获取任务配置详情成功，ID: {}", id);
                return Result.ok(taskConfig);
            } else {
                log.error("获取任务配置详情失败，ID: {}", id);
                return Result.fail("任务配置不存在");
            }
        } catch (Exception e) {
            log.error("获取任务配置详情异常: {}", e.getMessage(), e);
            return Result.fail("获取任务配置详情异常: " + e.getMessage());
        }
    }

    @Override
    public Result<List<TaskConfig>> getAllTaskConfigs() {
        try {
            // 获取所有任务配置列表
            List<TaskConfig> taskConfigs = list();
            log.info("获取所有任务配置列表成功，数量: {}", taskConfigs.size());
            return Result.ok(taskConfigs);
        } catch (Exception e) {
            log.error("获取所有任务配置列表异常: {}", e.getMessage(), e);
            return Result.fail("获取所有任务配置列表异常: " + e.getMessage());
        }
    }
}
