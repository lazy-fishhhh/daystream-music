package social.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageLikeVO<T> {
    /**
     * 列表
     */
    private List<T> dateList;
    /**
     * 是否还有更多
     */
    private Boolean hasMore;
    /**
     * 本页最后一个音乐的id
     */
    private String lastId;
}
