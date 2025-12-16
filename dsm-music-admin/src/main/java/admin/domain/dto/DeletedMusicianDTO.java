package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeletedMusicianDTO {
    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private List<String> ids;
}
