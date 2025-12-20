package com.future.music.entity.message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateUserInfoFirstTimeMessage {
    private Long userId;
    private Long taskId;

}
