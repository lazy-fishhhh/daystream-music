package admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
@Data
public class DeleteUserDTO {
    /**
     * 用户id
     */
    @NotNull(message = "id不能为空")
    private List<Long> userId;
}
