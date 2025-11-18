package social.domain.vo;

import lombok.Data;

@Data
public class CollectionVO {
    /**
     * 是否收藏
     */
    private Boolean isCollection;
    /**
     * 收藏量
     */
    private Long collectCount;
    /**
     * 状态描述
     */
    private String msg;
}
