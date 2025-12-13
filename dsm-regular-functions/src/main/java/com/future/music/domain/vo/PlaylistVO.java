package com.future.music.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaylistVO {
    private Long id;
    private String idStr;
    private String name;
    private String cover;
    /** 描述信息 */
    private String description;
    private Long playCount;
    private List<String> tagIds;
}
