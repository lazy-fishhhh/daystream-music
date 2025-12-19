package entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTaskVO {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private Long taskId;             // 关联task_config的任务ID
    private String taskName;
    private Integer taskStatus;      // 任务状态：0=未完成，1=已完成，2=已领奖
    private LocalDateTime completeTime;  // 任务完成时间（未完成则为NULL）
    private LocalDateTime rewardGetTime; // 奖励领取时间（未领奖则为NULL）
    private String taskDesc;  // 任务描述
    private String completeCondition; // 任务完成条件
    private Integer reward; // 任务音浪奖励

}
