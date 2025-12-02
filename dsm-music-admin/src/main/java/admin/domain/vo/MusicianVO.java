package admin.domain.vo;

import lombok.Data;

@Data
public class MusicianVO {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 音乐人艺名
     */
    private String nickname;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 状态 0-待审核 1-审核通过 2-审核驳回
     */
    private Integer status;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 音乐人头像
     */
    private String avatar;
}
