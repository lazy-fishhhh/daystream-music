package admin.domain.vo;

import lombok.Data;

@Data
public class BanUserStatusVO {
    /**
     * 状态描述
     */
    private String msg;
    /**
     * 是否封禁
     */
    private Boolean ban;
}
