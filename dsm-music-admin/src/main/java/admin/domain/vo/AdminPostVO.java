package admin.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminPostVO {
    /**
     * 动态id
     */
    private String id;
    /**
     * 作者id
     */
    private String authorId;
    /**
     * 作者名
     */
    private String authorName;
    /**
     * 作者头像
     */
    private String authorAvatar;
    /**
     * 作者身份
     */
    private String role;
    /**
     * 标题
     */
    private String title;
    /**
     * 动态内容
     */
    private String content;
    /**
     * 状态 0-正常 2-封禁下架
     */
    private Integer postStatus;
}
