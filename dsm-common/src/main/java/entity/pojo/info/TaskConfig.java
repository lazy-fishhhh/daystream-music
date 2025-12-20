package entity.pojo.info;

import annotation.IsZeroOrOne;
import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_config")
public class TaskConfig {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    @NotNull
    private String taskName;         // 任务名称
    private String taskDesc;        // 任务描述
    @NotNull
    private Integer rewardType;       // 奖励类型：1=音浪
    @IsZeroOrOne
    @NotNull
    private Integer status;      // 任务状态：0=禁用，1=启用
    @NotNull
    private Integer rewardValue;     // 奖励值
    @NotNull
    private String taskType;    // 任务类型：0=一次性任务，1=计数/时长类任务，2=操作类任务
    private String completeCondition;   // 任务完成条件
    @NotNull
    private Integer sort; // 排序 越小越靠前
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}
