package admin.domain.vo;

import lombok.Data;

@Data
public class DetailReportVO {
    /**
     *主键Id
     */
    private String id;
    /**
     * 举报人id
     */
    private String reporterId;
    /**
     * 举报人姓名
     */
    private String reporterName;
    /**
     * 举报人头像
     */
    private String reporterAvatar;
    /**
     * 举报详细内容
     */
    private String reportContent;
    /**
     * 被举报评论Id
     */
    private String targetId;
    /**
     * 被举报评论内容
     */
    private String targetContent;
    /**
     * 评论发布者id
     */
    private String authorId;
    /**
     * 发布者姓名
     */
    private String authorName;
    /**
     * 发布者头像
     */
    private String authorAvatar;
    /**
     * 举报来源 1-音乐 2-歌单 3-专辑 4-社区动态 5-mv
     */
    private Integer source;
    /**
     * 歌单标题/动态标题
     */
    private String title;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 状态：0-未处理 1-通过 2-驳回
     */
    private Integer status;
}
