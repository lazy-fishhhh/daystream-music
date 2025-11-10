package com.example.music.mapper;

import com.example.music.entity.pojo.music.PlayList;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MusicianMapper {


    @Insert("insert into playlist(playlist_name,cover_url,description,is_public,music_count,play_count,created_time,updated_time) values(#{name},#{cover},#{description},#{isPublic},#{musicCount},#{playCount},#{createdTime},#{updatedTime})")
   public void CreatePlayList(PlayList playList);
}
