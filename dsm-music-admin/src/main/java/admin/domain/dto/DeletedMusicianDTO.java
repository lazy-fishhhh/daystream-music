package admin.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeletedMusicianDTO {
    /**
     * 音乐人id
     */
    @NotNull(message = "音乐人id不能为空")
    private List<String> musicianIds;
}
