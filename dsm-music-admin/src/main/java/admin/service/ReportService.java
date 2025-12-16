package admin.service;

import admin.domain.dto.AdminMusicianApplyDTO;
import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.ReportDTO;
import admin.domain.vo.ReportVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.apache.ibatis.io.ResolverUtil;
import org.springframework.stereotype.Service;

public interface ReportService {
    Result<PageResp<ReportVO>> getReportList(ReportDTO reportDTO);

    Result handleReport(HandleReportDTO handleReportDTO);

    Result deleteReport(AdminMusicianApplyDTO adminMusicianApplyDTO);

    Result passReport(AdminMusicianApplyDTO adminMusicianApplyDTO);

    Result rejectReport(AdminMusicianApplyDTO adminMusicianApplyDTO);
}
