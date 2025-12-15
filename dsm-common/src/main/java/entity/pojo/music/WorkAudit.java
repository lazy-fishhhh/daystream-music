package entity.pojo.music;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品审核表（兼容音乐、MV 审核）
 */
@Data
@TableName("work_audit")
public class WorkAudit {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 审核记录ID
    private Integer targetType;      // 审核对象类型：1-音乐，2-MV
    private Long targetId;           // 审核对象ID（target_type=1→music.id；target_type=2→music_mv.id）
    private Long submitUserId;       // 提交审核的用户ID（音乐人ID，关联user.id）
    private LocalDateTime submitTime; // 提交审核时间
    private Long auditUserId;        // 审核人ID（管理员ID，关联user.id）
    private LocalDateTime auditTime; // 审核完成时间
    private Integer auditStatus;     // 审核状态：0-待审核，1-审核通过，2-审核驳回
    private String auditRemark;      // 审核备注（通过/驳回原因）
    private String rejectReason;     // 驳回原因（标准化，如：版权缺失、内容违规、音质不达标）
}