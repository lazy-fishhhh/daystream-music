package admin.mapper;

import admin.domain.vo.DetailReportVO;
import admin.domain.vo.ReportVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.UserReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface Report2Mapper extends BaseMapper<UserReport> {
    IPage<ReportVO> reportList(@Param("page") Page<ReportVO> page,
                               @Param("reason")Integer  reason,
                               @Param("key")String key,
                               @Param("status")Integer status);
    DetailReportVO getDetaiReport(@Param("reportId")Long reportId);

    @Update("update user_report set handler_id=#{userId},updated_time=now(),handle_status=#{status} where id=#{reportId}")
    void updatehandle(@Param("reportId")Long reportId,@Param("userId")Long userId,@Param("status")Integer status);
}
