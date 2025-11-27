package entity.pojo.search;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistory {
    private Long id;
    private Long userId;
    private String keyword;
    private LocalDateTime searchTime;
}
