package com.example.music.entity.pojo.util;

import lombok.Data;
import java.util.List;

@Data
public class PageResp<T> {
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 每页显示数量
     */
    private long size;
    
    /**
     * 当前页码
     */
    private long current;
    
    /**
     * 总页数
     */
    private long pages;
}