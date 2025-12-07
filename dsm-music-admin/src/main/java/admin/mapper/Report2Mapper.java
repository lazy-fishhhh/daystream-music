package admin.mapper;

import admin.domain.vo.ReportVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.UserReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface Report2Mapper extends BaseMapper<UserReport> {
    IPage<ReportVO> reportList(@Param("page") Page<ReportVO> page,
                               @Param("status")Integer status,
                               @Param("type")Integer type,
                               @Param("reason")Integer  reason,
                               @Param("key")String key);
}
