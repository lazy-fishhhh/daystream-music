package com.future.player.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.player.entity.vo.AlbumVO;
import entity.pojo.music.Album;
import entity.vo.MusicAlbumVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专辑 Mapper 接口（基于 MyBatis-Plus 扩展）
 */
@Mapper
public interface AlbumMapper extends BaseMapper<Album> {

    /**
     * 根据用户ID分页查询其创作的所有专辑（多表关联）
     * @param page 分页参数（MyBatis-Plus 分页对象）
     * @param userId 音乐人关联的用户ID
     * @return 分页的专辑 VO 列表
     */
    @Select("""
            SELECT a.id, a.album_name, m.stage_name as musician_name, a.cover_url,\s
                  a.release_date, a.introduction, a.status,\s
                  a.collect_count, a.comment_count
           FROM album a
               
           LEFT JOIN musician m\s
             ON a.musician_id = m.id\s
             AND m.user_id = #{userId}  -- 移到这里！
           ORDER BY a.release_date DESC
            """)
    IPage<AlbumVO> selectAlbumByUserId(
            Page<AlbumVO> page,  // 分页参数（必须作为第一个参数）
            @Param("userId") Long userId
    );

    @Select("""
            SELECT a.id, a.album_name, m.stage_name as musician_name, a.cover_url, 
                   a.release_date, a.introduction, a.status, 
                   a.collect_count, a.comment_count
            FROM album a
            LEFT JOIN musician m ON a.musician_id = m.id
            WHERE m.user_id = #{userId}
            ORDER BY a.release_date DESC
            """)
    Page<MusicAlbumVO> selectAllAlbumByUserId(
            Page<?> page,
            @Param("userId") Long userId
    );


    /**
     * 分页搜索专辑：关键词匹配专辑名 或 音乐人艺名
     * @param page 分页参数
     * @param key 搜索关键词（可为空，为空时返回所有上架专辑）
     * @return 分页专辑列表
     */
    @Select("<script>" +
            "SELECT a.id, a.album_name, m.stage_name as musician_name, a.cover_url, " +
            "       a.release_date, a.introduction, a.status, " +
            "       a.collect_count, a.comment_count " +
            "FROM album a " +
            "LEFT JOIN musician m ON a.musician_id = m.id " +
            "<where>" +
            "  <if test='key != null and key != &quot;&quot;'>" +  // XML 中双引号需正确转义
            "    (a.album_name LIKE CONCAT('%', #{key}, '%') OR m.stage_name LIKE CONCAT('%', #{key}, '%')) " +
            "  </if>" +
            "</where>" +
            "ORDER BY a.release_date DESC" +
            "</script>")
    IPage<AlbumVO> searchAlbumByKey(
            Page<AlbumVO> page,
            @Param("key") String key
    );

    @Select("""
            SELECT a.id, a.album_name, m.stage_name as musician_name, a.cover_url,\s
                     a.release_date, a.introduction, a.status,\s
                     a.collect_count, a.comment_count
              FROM album a
              INNER JOIN musician m ON a.musician_id = m.id
              WHERE a.status = 1
                AND a.musician_id IS NOT NULL
              ORDER BY IF(a.collect_count IS NULL, 1, 0), a.collect_count DESC,
                       IF(a.release_date IS NULL, 1, 0), a.release_date DESC  
            """)
    IPage<AlbumVO> selectAllPublicAlbum(Page<AlbumVO> page);
}