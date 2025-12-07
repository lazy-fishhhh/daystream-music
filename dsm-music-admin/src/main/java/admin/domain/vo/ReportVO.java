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
     * 被举报Id
     */
    private String targetId;
    /**
     * 被举报人姓名
     */
    private String targetName;
    /**
     * 被举报人头像
     */
    private String targetAvatar;
    /**
     * 被举报类型
     */
    private String targetType;
    /**
     * 举报理由
     */
    private String reason;
    /**
     * 举报内容
     */
    private String reportContent;
    /**
     * 创建时间
     */
    private String createdTime;
    /**
     * 状态：0-未处理 1-通过 2-驳回
     */
    private Integer status;
    /**
     * 目标状态 0-已下架/封禁 1-正常 2-注销
     */
    private Integer targetStatus;
}
