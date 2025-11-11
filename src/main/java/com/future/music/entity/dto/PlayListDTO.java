package com.future.music.entity.dto;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayListDTO {
    private Long id;
    @TableField("playlist_name")
    private String name;
    @TableField("cover_url")
    private String cover;
    private String description;
    private int isPublic;
}
