package com.future.music.entity.message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BindPhoneMessage {
    private Long userId;
    private Long taskId;

}
