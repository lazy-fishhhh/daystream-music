package entity.dto;

import lombok.Data;

@Data
public class SearchDTO {
    private String keyword;       // 搜索关键词（必填）
    private Integer type;         // 搜索类型：1-单曲，2-歌单，3-专辑，4-歌手，5-MV，6-用户
    private Integer pageNum = 1;  // 页码，默认第1页
    private Integer pageSize = 10; // 每页条数，默认10条
}