package com.future.music.domain.dto;

import lombok.Data;

import java.time.LocalDate;

/**
     * DTO类用于接收每日听歌次数
     */
@Data
  public  class DailyListenCountDTO {
        private LocalDate day;
        private Integer count;
        private Long seconds;
    }