package admin.service;

import admin.domain.dto.ReportDTO;
import admin.domain.vo.ReportVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.apache.ibatis.io.ResolverUtil;
import org.springframework.stereotype.Service;

@Service
public interface ReportService {
    Result<PageResp<ReportVO>> getReportList(ReportDTO reportDTO);
}
