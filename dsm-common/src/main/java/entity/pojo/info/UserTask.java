package entity.pojo.info;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_task")
public class UserTask {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 主键ID
    private Long userId;             // 用户ID
    private Long taskId;             // 关联task_config的任务ID
    private Integer taskStatus;      // 任务状态：0=未完成，1=已完成，2=已领奖
    private LocalDateTime completeTime;  // 任务完成时间（未完成则为NULL）
    private LocalDateTime rewardGetTime; // 奖励领取时间（未领奖则为NULL）
    private String extraData;        // 额外数据（如：听歌时长35分钟、头像URL等）
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}
