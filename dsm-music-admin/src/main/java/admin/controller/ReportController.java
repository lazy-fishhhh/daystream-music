package admin.controller;

import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.ReportDTO;
import admin.domain.vo.ReportVO;
import admin.service.ReportService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@Api(tags = "举报管理")
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

}
