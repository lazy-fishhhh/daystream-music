package com.future.music.domain.dto;

import java.time.LocalDate;

/**
     * DTO类用于接收每日听歌次数
     */
  public  class DailyListenCountDTO {
        private LocalDate day;
        private Integer count;
        
        // getter和setter方法
        public LocalDate getDay() { return day; }
        public void setDay(LocalDate day) { this.day = day; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }