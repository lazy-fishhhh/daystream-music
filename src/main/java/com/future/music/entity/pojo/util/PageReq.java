package com.future.music.entity.pojo.util;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class PageReq {
    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码不能小于1")
    private long current = 1;


    /**
     * 每页显示数量
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能大于100")
    private long size = 10;
}