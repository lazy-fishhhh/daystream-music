package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.entity.pojo.music.PlayList;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;



@Mapper
//  通过泛型，BaseMapper 能够明确知道当前 Mapper 操作的是哪个实体类
//  从而将数据库表与 Java 实体类关联起来
public interface MusicianMapper extends BaseMapper<PlayList> {


    @Insert("insert into playlist(playlist_name,cover_url,description,is_public,music_count,play_count,created_time,updated_time) values(#{name},#{cover},#{description},#{isPublic},#{musicCount},#{playCount},#{createdTime},#{updatedTime})")
   public void CreatePlayList(PlayList playList);
}
