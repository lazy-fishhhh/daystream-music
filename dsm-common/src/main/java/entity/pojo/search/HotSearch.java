package entity.pojo.search;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HotSearch {
    private Long id;
    private String keyword;
    private Integer searchCount;
    private Integer sortOrder;
    private LocalDateTime updateTime;
}
