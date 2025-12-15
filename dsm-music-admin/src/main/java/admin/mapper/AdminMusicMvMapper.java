package admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.music.MusicMv;
import entity.vo.MusicMvVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MV审核Mapper接口
 */
@Mapper
public interface AdminMusicMvMapper extends BaseMapper<MusicMv> {

    /**
     * 查询MV审核列表
     * @param page 分页参数
     * @param status 状态（0-待审核，1-上架，2-下架）
     * @return 分页的MV列表
     */
    @Select("<script>" +
            "SELECT mm.id, mm.mv_name, m.music_name, mm.duration, mm.cover_url, mm.release_time, " +
            "       mm.is_vip, mm.status, mu.stage_name AS musician_name " +
            "FROM music_mv mm " +
            "LEFT JOIN music m ON mm.music_id = m.id " +
            "LEFT JOIN musician mu ON m.musician_id = mu.id " +
            "WHERE 1=1 " +
            "<if test='status != null'>" +
            "    AND mm.status = #{status} " +
            "</if>" +
            "ORDER BY mm.create_time DESC " +
            "</script>")
    IPage<MusicMvVO> selectMvAuditList(@Param("page") Page<MusicMvVO> page, @Param("status") Integer status);

    /**
     * 查询单条MV审核详情
     * @param mvId MV ID
     * @return MV详情
     */
    @Select("<script>" +
            "SELECT mm.id, mm.mv_name, mm.music_id, m.music_name, mm.duration, mm.cover_url, mm.release_time, " +
            "       mm.is_vip, mm.status, mu.stage_name AS musician_name " +
            "FROM music_mv mm " +
            "LEFT JOIN music m ON mm.music_id = m.id " +
            "LEFT JOIN musician mu ON m.musician_id = mu.id " +
            "WHERE mm.id = #{mvId} " +
            "</script>")
    MusicMvVO selectMvAuditDetail(@Param("mvId") Long mvId);
}