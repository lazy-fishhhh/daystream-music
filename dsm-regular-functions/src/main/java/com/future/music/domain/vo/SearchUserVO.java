package com.future.music.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchUserVO {
    private Long id;
    private String userName;
    private String avatar;
    private int gender;
    private String introduction;
}
