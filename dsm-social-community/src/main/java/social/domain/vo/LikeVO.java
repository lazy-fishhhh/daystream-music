package social.domain.vo;

import lombok.Data;

@Data
public class LikeVO {
    /**
     * 是否点赞
     */
    private boolean islike;
    /**
     * 点赞量
     */
    private Long likecount;
    /**
     * 状态提示
     */
    private String msg;
}
