package admin.domain.vo;

import lombok.Data;

@Data
public class ReportVO {
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
     * 举报理由
     */
    private Integer reason;

    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 状态：0-未处理 1-通过 2-驳回
     */
    private Integer status;
}
