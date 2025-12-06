package com.future.music.domain.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInMessage {
        private boolean signInSuccess;
        private Integer reward;
        private Long userId;
        private LocalDateTime signInTime;
}
