package com.future.login.mapper;


import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select id from user")
    List<Long> selectAllUserId();
    @Select("select wallet_balance from user where id = #{userId}")
    Integer selectMyWaves(@Param("userId")Long userId);

    @Select("select role_id from user_role where user_id = #{userId}")
    Integer selectRoleId(Long userId);

    @Select("insert into playlist(user_id, playlist_name, cover_url, description, is_public) values(#{id},#{playlistName}, #{coverUrl}, #{description}, 0)")
    void createPlaylist(Long id, String playlistName, String coverUrl, String description);
}
