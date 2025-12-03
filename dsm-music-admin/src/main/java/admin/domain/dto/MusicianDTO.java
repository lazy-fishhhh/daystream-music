package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data

public class MusicianDTO {
    /**
     *  音乐人
     **/
    @NotNull(message = "音乐人id不能为空")
     private String id;
     /**
     *审批状态 0:待审核 1:通过 2:拒绝
     */
     @NotNull(message = "审批状态不能为空")
    private Integer status;
}
