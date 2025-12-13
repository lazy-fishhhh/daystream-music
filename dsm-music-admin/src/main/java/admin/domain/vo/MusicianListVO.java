package admin.domain.vo;

import lombok.Data;

@Data
public class MusicianListVO {
    /**
     * 音乐人
     */
    private String id;
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
     * 状态 0-正常 1-封禁
     */
    private Integer status;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 音乐人简介
     */
    private String introduction;
    /**
     * 音乐人头像
     */
    private String avatar;
}
