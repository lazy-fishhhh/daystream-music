package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.SearchUserVO;
import entity.pojo.social.UserCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserCollectionMapper extends BaseMapper<UserCollection> {

    /**
     * 分页查询收藏指定歌单的用户列表
     * @param playlistId 歌单ID
     * @return 收藏用户信息列表
     */
    @Select("SELECT u.id, u.username, u.avatar, u.gender, u.introduction " +
            "FROM user_collection uc " +
            "INNER JOIN user u ON uc.user_id = u.id " +
            "WHERE uc.target_type = 2 " +
            "  AND uc.target_id = #{playlistId} " +
            "  AND uc.is_canceled = 0 " +
            "ORDER BY uc.updated_time DESC") // 建议按收藏时间倒序，更符合业务需求
    IPage<SearchUserVO> selectPlaylistCollectors(Page<SearchUserVO> page, @Param("playlistId") Long playlistId);

}
