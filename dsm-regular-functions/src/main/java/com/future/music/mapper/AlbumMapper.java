package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 分页搜索专辑：关键词匹配专辑名 或 音乐人艺名
     * @param page 分页参数
     * @param key 搜索关键词（可为空，为空时返回所有上架专辑）
     * @return 分页专辑列表
     */
    @Select("<script>" +
            "SELECT a.* FROM album a " +
            "INNER JOIN musician m ON a.musician_id = m.id " +
            "WHERE a.status = 1 " +
            // 关键词非空时，匹配专辑名或音乐人艺名
            "<if test='key != null and key != \"\"'>" +
            "AND (a.album_name LIKE CONCAT('%', #{key}, '%') OR m.stage_name LIKE CONCAT('%', #{key}, '%')) " +
            "</if>" +
            // 按发行时间降序（可选，根据业务调整）
            "ORDER BY a.release_date DESC" +
            "</script>")
    IPage<Album> searchAlbumByKey(
            Page<Album> page,
            @Param("key") String key
    );


}
