package admin.service.impl;

import admin.domain.dto.ReportDTO;
import admin.domain.vo.ReportVO;
import admin.mapper.Report2Mapper;
import admin.service.ReportService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private Report2Mapper reportMapper;
    @Override
    public Result<PageResp<ReportVO>> getReportList(ReportDTO reportDTO) {
        //构建分页
        Page<ReportVO> page =new Page<>(reportDTO.getPageNum(),reportDTO.getPageSize());
        IPage<ReportVO> reportVOIPage = reportMapper.reportList(page, reportDTO.getStatus(), reportDTO.getType(), reportDTO.getReason(), reportDTO.getKey());
        PageResp<ReportVO> pageResp = new PageResp<>();
        pageResp.setTotal(reportVOIPage.getTotal());
        pageResp.setPages(reportVOIPage.getPages());
        pageResp.setRecords(reportVOIPage.getRecords());
        pageResp.setCurrent(reportVOIPage.getCurrent());
        pageResp.setPages(reportVOIPage.getPages());
        return Result.ok(pageResp);

    }
}
