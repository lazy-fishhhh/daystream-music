package admin.mapper;

import admin.domain.vo.MusicVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import entity.pojo.music.Music;
import entity.pojo.music.MusicAudio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminMusicMapper extends BaseMapper<Music> {
    /**
     * 查询音乐审核列表
     * @param page 分页对象
     * @param status 审核状态（0-待审核，1-通过，2-驳回，null-全部）
     * @param keyword 关键字（模糊查询，匹配歌曲名或音乐人名）
     * @return 分页的音乐审核列表
     */
    @Select({
            "<script>",
            "SELECT m.id, m.music_name, m.cover_url, m.duration, m.create_time, m.is_vip, ",
            "       m.status, m.musician_id, m.album_id,",
            "       a.album_name, mu.stage_name AS musician_name,",
            "       wa.audit_status, wa.audit_remark, wa.reject_reason",
            "FROM work_audit wa",
            "LEFT JOIN music m ON wa.target_id = m.id",
            "LEFT JOIN album a ON m.album_id = a.id",
            "LEFT JOIN musician mu ON m.musician_id = mu.id",
            "WHERE wa.target_type = 1",
            "<if test='status != null'>",
            "    AND wa.audit_status = #{status}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "    AND (m.music_name LIKE CONCAT('%', #{keyword}, '%') OR mu.stage_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY wa.audit_time DESC",
            "</script>"
    })
    IPage<MusicVO> selectMusicAuditList(IPage<MusicVO> page, 
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword);
    
    /**
     * 根据音乐ID查询音频信息
     * @param musicId 音乐ID
     * @return 音频信息列表
     */
    @Select("SELECT * FROM music_audio_relation WHERE music_id = #{musicId}")
    List<MusicAudio> selectAudioByMusicId(@Param("musicId") Long musicId);
    
    /**
     * 检查用户是否点赞过音乐
     * @param userId 用户ID
     * @param musicId 音乐ID
     * @return 1-已点赞，0-未点赞
     */
    @Select("SELECT COUNT(*) FROM like_record WHERE user_id = #{userId} AND target_id = #{musicId} AND target_type = 1")
    Integer checkMusicLiked(@Param("userId") Long userId, @Param("musicId") Long musicId);
    
    /**
     * 查询音乐的驳回原因
     * @param musicId 音乐ID
     * @return 驳回原因
     */
    @Select("SELECT reject_reason FROM work_audit WHERE work_id = #{musicId} AND work_type = 1")
    String getMusicRejectReason(@Param("musicId") Long musicId);
}
