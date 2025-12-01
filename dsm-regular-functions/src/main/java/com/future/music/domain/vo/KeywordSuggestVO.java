package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeywordSuggestVO {
    private List<String> suggestList; // 联想关键词列表（去重后）
}