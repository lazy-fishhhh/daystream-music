package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.UserReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper extends BaseMapper<UserReport> {
}
