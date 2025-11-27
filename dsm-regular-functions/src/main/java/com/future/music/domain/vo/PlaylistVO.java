package com.future.music.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlaylistVO {
    private Long id;
    private String name;
    private String cover;
    /** 描述信息 */
    private String description;
    private Long playCount;
}
