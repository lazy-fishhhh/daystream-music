package entity.pojo.info;

import lombok.Data;
import net.bytebuddy.asm.Advice;

import java.time.LocalDateTime;
@Data
public class UserReport {
    private Long id;//主键id
    private Long reporterId;//举报人id
    private Integer targetType;//举报目标类型
    private Long targetId;//举报目标id
    private  Integer reportReason; //举报原因
    private String reportContent;//举报内容
    private Integer handleStatus;//处理状态
    private Long handlerId;//处理人id
    private LocalDateTime createdTime; //创建时间
    private LocalDateTime updatedTime; //更新时间
}
