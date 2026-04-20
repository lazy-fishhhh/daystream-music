package admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.music.MusicMv;
import entity.vo.MusicMvVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * MV审核Mapper接口
 */
@Mapper
public interface AdminMusicMvMapper extends BaseMapper<MusicMv> {

    /**
     * 查询MV审核列表
     * @param page 分页参数
     * @param status 审核状态（0-待审核，1-通过，2-驳回，null-全部）
     * @return 分页的MV列表
     */
    @Select("<script>" +
            "SELECT mm.id, mm.mv_name, m.music_name, mm.duration, mm.cover_url, mm.release_time, " +
            "       mm.is_vip, mm.status, mu.stage_name AS musician_name, " +
            "       wa.audit_status, wa.audit_remark, wa.reject_reason " +
            "FROM work_audit wa " +
            "LEFT JOIN music_mv mm ON wa.target_id = mm.id " +
            "LEFT JOIN music m ON mm.music_id = m.id " +
            "LEFT JOIN musician mu ON m.musician_id = mu.id " +
            "WHERE wa.target_type = 2 " +
            "<if test='status != null'>" +
            "    AND wa.audit_status = #{status} " +
            "</if>" +
            "ORDER BY wa.audit_time DESC " +
            "</script>")
    IPage<MusicMvVO> selectMvAuditList(@Param("page") Page<MusicMvVO> page, @Param("status") Integer status);

    /**
     * 查询单条MV审核详情
     * @param mvId MV ID
     * @return MV详情
     */
    @Select("<script>" +
            "SELECT mm.id, mm.mv_name, mm.music_id, m.music_name, mm.duration, mm.cover_url, mm.release_time, " +
            "       mm.is_vip, mm.status, mu.stage_name AS musician_name, " +
            "       wa.audit_status, wa.audit_remark, wa.reject_reason " +
            "FROM music_mv mm " +
            "LEFT JOIN music m ON mm.music_id = m.id " +
            "LEFT JOIN musician mu ON m.musician_id = mu.id " +
            "LEFT JOIN work_audit wa ON wa.target_id = mm.id AND wa.target_type = 2 " +
            "WHERE mm.id = #{mvId} " +
            "</script>")
    MusicMvVO selectMvAuditDetail(@Param("mvId") Long mvId);
    
    /**
     * 更新MV审核状态
     * @param mvId MV ID
     * @param auditStatus 审核状态
     * @param rejectReason 驳回原因
     * @return 更新结果
     */
    @Update("<script>" +
            "UPDATE work_audit SET audit_status = #{auditStatus}, audit_remark = #{rejectReason}, reject_reason = #{rejectReason}, audit_time = NOW() " +
            "WHERE target_id = #{mvId} AND target_type = 2 " +
            "</script>")
    int updateMvAuditStatus(@Param("mvId") Long mvId, @Param("auditStatus") Integer auditStatus, @Param("rejectReason") String rejectReason);
    
    /**
     * 批量更新MV审核状态
     * @param mvIds MV ID列表
     * @param auditStatus 审核状态
     * @param rejectReason 驳回原因
     * @return 更新结果
     */
    @Update("<script>" +
            "UPDATE work_audit SET audit_status = #{auditStatus}, audit_remark = #{rejectReason}, reject_reason = #{rejectReason}, audit_time = NOW() " +
            "WHERE target_id IN <foreach collection='mvIds' item='mvId' open='(' separator=',' close=')'>#{mvId}</foreach> AND target_type = 2 " +
            "</script>")
    int batchUpdateMvAuditStatus(@Param("mvIds") List<Long> mvIds, @Param("auditStatus") Integer auditStatus, @Param("rejectReason") String rejectReason);
}