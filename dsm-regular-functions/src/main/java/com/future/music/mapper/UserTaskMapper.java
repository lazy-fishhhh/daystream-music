package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import entity.pojo.info.UserTask;
import entity.vo.UserTaskVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserTaskMapper extends BaseMapper<UserTask> {
    // 根据用户ID和任务ID查询用户任务
    default UserTask selectByUserIdAndTaskId(Long userId, Long taskId) {
        return selectOne(new QueryWrapper<UserTask>()
                .eq("user_id", userId)
                .eq("task_id", taskId));
    }

    // 批量插入用户任务
    @Insert("<script>INSERT INTO user_task (user_id, task_id, task_status, complete_time, reward_get_time, extra_data, create_time, update_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.taskId}, #{item.taskStatus}, #{item.completeTime}, #{item.rewardGetTime}, #{item.extraData}, #{item.createTime}, #{item.updateTime})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<UserTask> userTasks);

    // 连表查询用户任务列表（用户任务表和任务配置表）
    @Select("SELECT ut.id, " +
            "ut.task_id, " +
            "tc.task_name, " +
            "ut.task_status, " +
            "ut.complete_time, " +
            "ut.reward_get_time, " +
            "tc.task_desc, " +
            "tc.complete_condition, " +
            "tc.reward_value AS reward " +
            "FROM user_task ut " +
            "LEFT JOIN task_config tc ON ut.task_id = tc.id " +
            "WHERE ut.user_id = #{userId} " +
            "ORDER BY ut.create_time ASC")
    List<UserTaskVO> selectUserTaskVOByUserId(@Param("userId") Long userId);

    // 连表查询用户任务列表（支持分页）
    @Select("SELECT ut.id, " +
            "ut.task_id, " +
            "tc.task_name, " +
            "ut.task_status, " +
            "ut.complete_time, " +
            "ut.reward_get_time, " +
            "tc.task_desc, " +
            "tc.complete_condition, " +
            "tc.reward_value AS reward " +
            "FROM user_task ut " +
            "LEFT JOIN task_config tc ON ut.task_id = tc.id " +
            "WHERE ut.user_id = #{userId} " +
            "ORDER BY ut.create_time ASC")
    List<UserTaskVO> selectUserTaskVOByUserIdWithPage(@Param("userId") Long userId, @Param("page") com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserTaskVO> page);
}