package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.domain.vo.AlbumVO;
import entity.pojo.music.Album;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
    /**
     * 根据用户ID查询其创作的所有专辑（多表关联查询）
     * @param userId 音乐人关联的用户ID
     * @return 专辑ID和名称列表
     */
    @Select("""
            SELECT a.id, a.album_name
            FROM musician m
            INNER JOIN album a ON m.id = a.musician_id  -- 关联音乐人表和专辑表
            WHERE m.user_id = #{userId}                -- 筛选指定用户的音乐人数据
            """)
    List<AlbumVO> selectAlbumByUserId(@Param("userId") Long userId);
}
