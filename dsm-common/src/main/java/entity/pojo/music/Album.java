package entity.pojo.music;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Album {
    private Long id;
    private Long musicianId;
    private String albumName;
    private String coverUrl;
    private LocalDateTime releaseDate;  //发行日期
    private String introduction;        //简介
    private Integer status;             //状态（0-下架，1-上架）
    private Long collectCount;          //收藏量
    private Long commentCount;          //评论量
}
