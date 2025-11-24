package social.domain.vo;

import lombok.Data;

@Data
public class FollowerVO {
    /**
     * 是否关注
     */
    private boolean isfollow;
    /**
     * 状态描述
     */
    private String msg;
}
