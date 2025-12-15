package admin.mapper;

import admin.domain.vo.MusicVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import entity.pojo.music.Music;
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
     * @return 分页的音乐审核列表
     */
    @Select({
            "<script>",
            "SELECT m.id, m.music_name, m.cover_url, m.duration, m.create_time, m.is_vip, ",
            "       m.status,",
            "       a.album_name, mu.stage_name AS musician_name",
            "FROM music m",
            "LEFT JOIN album a ON m.album_id = a.id",
            "LEFT JOIN musician mu ON m.musician_id = mu.id",
            "WHERE 1=1", // 防止 status=null 时，SQL 错误 ：  where 后面没了
            "<if test='status != null'>",
            "    AND m.status = #{status}",
            "</if>",
            "ORDER BY m.create_time DESC",
            "</script>"
    })
    IPage<MusicVO> selectMusicAuditList(IPage<MusicVO> page, @Param("status") Integer status);
}
