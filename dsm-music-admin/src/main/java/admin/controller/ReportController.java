package admin.controller;

import admin.domain.dto.AdminMusicianApplyDTO;
import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.ReportDTO;
import admin.domain.vo.DetailReportVO;
import admin.domain.vo.ReportVO;
import admin.service.ReportService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@Api(tags = "举报管理")
@PreAuthorize("hasRole('管理员')")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @PostMapping("/getReportList")
    @ApiOperation(value = "获取举报列表")
    public Result<PageResp<ReportVO>> getReportList(@Valid @RequestBody ReportDTO reportDTO) {
        return reportService.getReportList(reportDTO);
    }
    @PostMapping("/handleReport")
    @ApiOperation(value = "处理举报")
    public Result handleReport(@Valid @RequestBody HandleReportDTO handle) {
        return reportService.handleReport(handle);
    }
    @GetMapping("/getDetaiReport")
    @ApiOperation(value = "获取举报详情")
    public Result<DetailReportVO> getDetaiReport(@NotBlank(message = "id不能为空") @Valid @RequestParam(value = "id", required = true)String id) {
        return reportService.getDetaiReport(id);
    }
    @PostMapping("/deleteReport")
    @ApiOperation(value = "批量删除举报,仅限未处理的举报")
    public Result deleteReport(@Valid @RequestBody AdminMusicianApplyDTO adminMusicianApplyDTO) {
        return reportService.deleteReport(adminMusicianApplyDTO);
    }
    @PostMapping("/passReport")
    @ApiOperation(value = "批量通过,仅限未处理的举报")
    public Result passReport(@Valid @RequestBody AdminMusicianApplyDTO adminMusicianApplyDTO) {
        return reportService.passReport(adminMusicianApplyDTO);
    }
    @PostMapping("/rejectReport")
    @ApiOperation(value = "批量驳回,仅限未处理的举报")
    public Result rejectReport(@Valid @RequestBody AdminMusicianApplyDTO adminMusicianApplyDTO) {
        return reportService.rejectReport(adminMusicianApplyDTO);
    }

}
