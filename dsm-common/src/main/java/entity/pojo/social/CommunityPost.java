package entity.pojo.social;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityPost {
    @TableId(type = IdType.AUTO)
    private Long id; //主键id
    private Long userId; //发布用户id
    private String title;//标题
    private String content;//内容
    private Integer isDeleted; //是否删除 0-否 1-是
    private Integer status; //0-待审核 1-审核通过 2-审核驳回
    private Integer commentCount;//评论数
    private Integer likeCount;//点赞数
    private LocalDateTime createTime;//创建时间
    private LocalDateTime deleteTime;//删除时间
    private LocalDateTime updatedTime;
}
