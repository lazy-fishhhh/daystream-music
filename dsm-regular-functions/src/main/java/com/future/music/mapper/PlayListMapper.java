package com.future.music.mapper;


import cn.hutool.db.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.music.Playlist;
import org.mapstruct.Mapper;

@Mapper
//  通过泛型，BaseMapper 能够明确知道当前 Mapper 操作的是哪个实体类
//  从而将数据库表与 Java 实体类关联起来
public interface PlayListMapper extends BaseMapper<Playlist> {
    void selectPlayList(Page page, LambdaQueryWrapper<Playlist> lqw);
}
