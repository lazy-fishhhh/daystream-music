package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.TaskConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskConfigMapper extends BaseMapper<TaskConfig> {
    /**
     * 连表删除：删除 user_task 中关联 task_config.target_type = 3 的记录
     */
    @Delete("""
        DELETE ut 
        FROM user_task ut
        INNER JOIN task_config tc ON ut.task_id = tc.id
        WHERE tc.task_type = 3
    """)
    int deleteByTargetType3();
}
