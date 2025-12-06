package admin.service.impl;

import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.ReportDTO;
import admin.domain.vo.ReportVO;
import admin.mapper.Report2Mapper;
import admin.mapper.UserRole3Mapper;
import admin.service.ReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.UserReport;
import entity.pojo.music.Music;
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
/*
    @Override
    public Result handleReport(HandleReportDTO handleReportDTO) {
        //获取举报id
        Long reportId = Long.parseLong(handleReportDTO.getReportId());
        Long targetId=Long.parseLong(handleReportDTO.getTargetId());
        Integer targetType=handleReportDTO.getTargetType();
        LambdaQueryWrapper<UserReport> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(UserReport::getId,reportId);
        UserReport userReport = reportMapper.selectOne(queryWrapper);
        if(userReport==null){
            return Result.fail("举报不存在");
        }
        Integer handleStatus=userReport.getHandleStatus();
        //如果通过了举报
        if(handleStatus==1){
            //根据类型处理，1-音乐 2-评论 3-用户 4-音乐人
            if(targetType==1){
                //处理音乐，让音乐下架
                LambdaQueryWrapper<Music> musicWrapper=new LambdaQueryWrapper();
                musicWrapper.eq(Music::getId,targetId);


            }
        }


    }*/
}
