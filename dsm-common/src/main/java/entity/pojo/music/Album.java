package entity.pojo.music;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Album {
    private Long id;
    @ApiModelProperty("专辑名称")
    private String albumName;
    @ApiModelProperty("作者id")
    private Long musicianId;
    @ApiModelProperty("专辑封面url")
    private String coverUrl;
    @ApiModelProperty("发行时间")
    private LocalDateTime releaseDate;  //发行日期
    @ApiModelProperty("简介")
    private String introduction;        //简介
    @ApiModelProperty( value = "状态",
                       notes = "0-下架，1-上架")
    private Integer status;             //状态（0-下架，1-上架）
    @ApiModelProperty("收藏量")
    private Long collectCount;          //收藏量
    @ApiModelProperty("评论量")
    private Long commentCount;          //评论量

    @ApiModelProperty("歌曲总数")
    private Long musicCount;
}
