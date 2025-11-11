package entity.pojo.music;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayList {
        private Long id;
        private Long userId;
        @TableField("playlist_name")
        private String name;
        @TableField("cover_url")
        private String cover;
        private String description;
        private int isPublic;
        private Long musicCount;
        private Long playCount;
        @TableField("created_time")
        private LocalDateTime createdTime;
        @TableField("updated_time")
        private LocalDateTime updatedTime;
}
