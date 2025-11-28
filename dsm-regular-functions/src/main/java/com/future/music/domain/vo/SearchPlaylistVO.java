package com.future.music.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchPlaylistVO {
    private Long id;
    private Long userId;
    private String name;
    private String coverUrl;
    private Long musicCount;
    private Long playCount;

    private String userName;
}
