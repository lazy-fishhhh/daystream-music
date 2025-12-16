package admin.service.impl;

import admin.domain.dto.AdminMusicianApplyDTO;
import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.ReportDTO;
import admin.domain.vo.ReportVO;
import admin.mapper.AdminMusicMapper;
import admin.mapper.AdminMusicianMapper;
import admin.mapper.Report2Mapper;
import admin.mapper.UserRole3Mapper;
import admin.service.ReportService;
import admin.service.UpdateUserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.mapper.UserMapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.info.User;
import entity.pojo.info.UserReport;
import entity.pojo.info.UserRole;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.social.Comment;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import social.mapper.CommentMapper;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private Report2Mapper reportMapper;
    @Autowired
    private AdminMusicianMapper adminMusicianMapper;
    @Autowired
    private AdminMusicMapper adminMusicMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UpdateUserInfoService updateUserInfoService;
    @Autowired
    private UserRole3Mapper userRole3Mapper;
    @Override
    public Result<PageResp<ReportVO>> getReportList(ReportDTO reportDTO) {
        //构建分页
        Page<ReportVO> page =new Page<>(reportDTO.getPageNum(),reportDTO.getPageSize());
        IPage<ReportVO> reportVOIPage = reportMapper.reportList(page, reportDTO.getType(), reportDTO.getReason(), reportDTO.getKey(),reportDTO.getStatus());
        PageResp<ReportVO> pageResp = new PageResp<>();
        pageResp.setTotal(reportVOIPage.getTotal());
        pageResp.setPages(reportVOIPage.getPages());
        pageResp.setRecords(reportVOIPage.getRecords());
        pageResp.setCurrent(reportVOIPage.getCurrent());
        pageResp.setPages(reportVOIPage.getPages());
        return Result.ok(pageResp);

    }

    @Override
    public Result handleReport(HandleReportDTO handleReportDTO) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        //获取举报id
        Long reportId = Long.parseLong(handleReportDTO.getReportId());
        LambdaQueryWrapper<UserReport> reportQueryWrapper = new LambdaQueryWrapper<>();
        reportQueryWrapper.eq(UserReport::getId,reportId);
        UserReport userReport = reportMapper.selectOne(reportQueryWrapper);
        if(userReport==null)
        {
            return Result.fail("举报不存在");
        }
        Long targetId=userReport.getTargetId();
        Integer targetType=userReport.getTargetType();
        Integer handleResult= handleReportDTO.getHandleResult();
        //如果通过了举报
        if(handleResult==1){
            //根据类型处理，1-音乐 2-评论 3-用户 4-音乐人
            if(targetType==1){
                //处理音乐，让音乐下架
                LambdaQueryWrapper<Music> musicWrapper=new LambdaQueryWrapper();
                musicWrapper.eq(Music::getId,targetId);
                Music music = adminMusicMapper.selectOne(musicWrapper);
                if(music==null)
                {
                    return Result.ok("该音乐已删除,无法操作");
                }
                //否则的话就下架音乐
                UpdateWrapper<Music> updateWrapper=new UpdateWrapper<>();
                updateWrapper.eq("id",targetId)
                        .set("status",2);
                adminMusicMapper.update(updateWrapper);
                reportMapper.updatehandle(reportId,currentUserId,handleResult);
                return Result.ok("音乐下架成功");
            }
            else if(targetType==2) {
                //判断评论是否存在
                LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper();
                commentWrapper.eq(Comment::getId, targetId);
                Comment comment = commentMapper.selectOne(commentWrapper);
                if (comment == null) {
                    return Result.ok("该评论已删除,无法操作");
                }
                //把评论删除
                UpdateWrapper<Comment> commentUpdateWrapper = new UpdateWrapper<>();
                commentUpdateWrapper.eq("id", targetId)
                        .set("status", 0);
                commentMapper.update(commentUpdateWrapper);
                reportMapper.updatehandle(reportId,currentUserId,handleResult);
                return Result.ok("评论删除成功");
            }else if (targetType == 3)
            {
                //判断用户是否存在
                LambdaQueryWrapper<User> userWrapper=new LambdaQueryWrapper<>();
                userWrapper.eq(User::getId,targetId);
                User user = userMapper.selectOne(userWrapper);
                if(user==null)
                {
                    return Result.ok("该用户不存在,无法操作");
                }
                userWrapper.eq(User::getUserStatus,2);
                User user1 = userMapper.selectOne(userWrapper);
                if(user1!=null)
                {
                    return Result.ok("该用户已注销,无法操作");
                }
                //把用户封禁
                UpdateWrapper<User> userUpdateWrapper=new UpdateWrapper<>();
                userUpdateWrapper.eq("id",targetId)
                        .set("status",0);
                userMapper.update(userUpdateWrapper);
                reportMapper.updatehandle(reportId,currentUserId,handleResult);
                return Result.ok("用户封禁成功");
            }else if(targetType==4)
            {
                //判断音乐人
                LambdaQueryWrapper<Musician> musicianWrapper=new LambdaQueryWrapper<>();
                musicianWrapper.eq(Musician::getUserId,targetId)
               .in(Musician::getStatus,0,2);
                if(adminMusicianMapper.selectOne(musicianWrapper)!=null)
                {
                    return Result.fail("音乐人不存在");
                }
                LambdaQueryWrapper<Musician> musicianWrapper1=new LambdaQueryWrapper<>();
                musicianWrapper1.eq(Musician::getUserId,targetId)
                        .eq(Musician::getStatus,1)
                        .eq(Musician::getIsDeleted,2);
                if(adminMusicianMapper.selectOne(musicianWrapper1)!=null)
                {
                    return Result.fail("该音乐人已注销，无法操作");
                }
                //封禁音乐人
                UpdateWrapper<Musician> musicianUpdateWrapper=new UpdateWrapper<>();
                musicianUpdateWrapper.eq("user_id",targetId)
                        .set("is_deleted",1);
                adminMusicianMapper.update(musicianUpdateWrapper);
                //把音乐人身份更新成普通人
                    UpdateWrapper<UserRole> userRoleUpdateWrapper = new UpdateWrapper<>();
                    userRoleUpdateWrapper.eq("user_id", targetId)
                            .set("role_id", 1);
                    userRole3Mapper.update(userRoleUpdateWrapper);
                    //变更redis中的权限
                updateUserInfoService.updateUserInfo(targetId);
                reportMapper.updatehandle(reportId,currentUserId,handleResult);
                return Result.ok("音乐人封禁成功");
            }
        }
        reportMapper.updatehandle(reportId,currentUserId,handleResult);
        return Result.ok("举报已驳回");
    }

    @Override
    public Result deleteReport(AdminMusicianApplyDTO adminMusicianApplyDTO) {
        //删除未处理的举报
        List<String> ids=adminMusicianApplyDTO.getApplyId();
        //传过来的都是同类型的,查询一个判断是否是未处理的
        LambdaQueryWrapper<UserReport> reportWrapper=new LambdaQueryWrapper<>();
        reportWrapper.eq(UserReport::getId,Long.parseLong(ids.get(0)))
                .in(UserReport::getHandleStatus,1,2);
        UserReport userReport = reportMapper.selectOne(reportWrapper);
        if(userReport!= null) {
            return Result.fail("该举报已处理,无法删除");

        }
        List<Long> idList=ids.stream().map(Long::parseLong).collect(Collectors.toList());
        //删除
        reportMapper.deleteBatchIds(idList);
        return Result.ok("删除成功");
    }

    @Override
    public Result passReport(AdminMusicianApplyDTO adminMusicianApplyDTO) {
        //批量通过
        try {
            List<String> ids = adminMusicianApplyDTO.getApplyId();
            List<Long> idList = ids.stream().map(Long::parseLong).collect(Collectors.toList());
            //查询一个
            LambdaQueryWrapper<UserReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(UserReport::getId, idList.get(0))
                    .in(UserReport::getHandleStatus, 1, 2);
            UserReport userReport = reportMapper.selectOne(reportWrapper);
            if (userReport != null) {
                return Result.fail("举报已处理,通过失败");
            }
            Long currentUserId = SecurityUtil.getCurrentUserId();
            //批量处理
            for (Long id : idList) {
                LambdaQueryWrapper<UserReport> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserReport::getId, id);
                UserReport report = reportMapper.selectOne(wrapper);
                if (report == null) {
                    continue;
                }
                Integer targetType = report.getTargetType();
                Long targetId = report.getTargetId();
                //根据类型处理，1-音乐 2-评论 3-用户 4-音乐人
                if (targetType == 1) {
                    //处理音乐，让音乐下架
                    LambdaQueryWrapper<Music> musicWrapper = new LambdaQueryWrapper();
                    musicWrapper.eq(Music::getId, targetId);
                    Music music = adminMusicMapper.selectOne(musicWrapper);
                    if (music == null) {
                        continue;
                    }
                    //否则的话就下架音乐
                    UpdateWrapper<Music> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("id", targetId)
                            .set("status", 2);
                    adminMusicMapper.update(updateWrapper);
                    reportMapper.updatehandle(id, currentUserId, 1);
                    continue;
                } else if (targetType == 2) {
                    //判断评论是否存在
                    LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper();
                    commentWrapper.eq(Comment::getId, targetId);
                    Comment comment = commentMapper.selectOne(commentWrapper);
                    if (comment == null) {
                        continue;
                    }
                    //把评论删除
                    UpdateWrapper<Comment> commentUpdateWrapper = new UpdateWrapper<>();
                    commentUpdateWrapper.eq("id", targetId)
                            .set("status", 0);
                    commentMapper.update(commentUpdateWrapper);
                    reportMapper.updatehandle(id, currentUserId, 1);
                } else if (targetType == 3) {
                    //判断用户是否存在
                    LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
                    userWrapper.eq(User::getId, targetId);
                    User user = userMapper.selectOne(userWrapper);
                    if (user == null) {
                        continue;
                    }
                    userWrapper.eq(User::getUserStatus, 2);
                    User user1 = userMapper.selectOne(userWrapper);
                    if (user1 != null) {
                        continue;
                    }
                    //把用户封禁
                    UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
                    userUpdateWrapper.eq("id", targetId)
                            .set("status", 0);
                    userMapper.update(userUpdateWrapper);
                    reportMapper.updatehandle(id, currentUserId, 1);
                } else if (targetType == 4) {
                    //判断音乐人
                    LambdaQueryWrapper<Musician> musicianWrapper = new LambdaQueryWrapper<>();
                    musicianWrapper.eq(Musician::getUserId, targetId)
                            .in(Musician::getStatus, 0, 2);
                    if (adminMusicianMapper.selectOne(musicianWrapper) != null) {
                        continue;
                    }
                    LambdaQueryWrapper<Musician> musicianWrapper1 = new LambdaQueryWrapper<>();
                    musicianWrapper1.eq(Musician::getUserId, targetId)
                            .eq(Musician::getStatus, 1)
                            .eq(Musician::getIsDeleted, 2);
                    if (adminMusicianMapper.selectOne(musicianWrapper1) != null) {
                        continue;
                    }
                    //封禁音乐人
                    UpdateWrapper<Musician> musicianUpdateWrapper = new UpdateWrapper<>();
                    musicianUpdateWrapper.eq("user_id", targetId)
                            .set("is_deleted", 1);
                    adminMusicianMapper.update(musicianUpdateWrapper);
                    //把音乐人身份更新成普通人
                    UpdateWrapper<UserRole> userRoleUpdateWrapper = new UpdateWrapper<>();
                    userRoleUpdateWrapper.eq("user_id", targetId)
                            .set("role_id", 1);
                    userRole3Mapper.update(userRoleUpdateWrapper);
                    //变更redis中的权限
                    updateUserInfoService.updateUserInfo(targetId);
                    reportMapper.updatehandle(id, currentUserId, 1);
                }
            }
            return Result.ok("批量通过成功");
        }catch(Exception e){
                log.error("批量通过失败", e);
                return Result.fail("操作失败");
            }
    }
//批量驳回
    @Override
    public Result rejectReport(AdminMusicianApplyDTO adminMusicianApplyDTO) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        //把申请设置为驳回
        try {
            List<String> idList = adminMusicianApplyDTO.getApplyId();
            List<Long> ids= idList.stream().map(id -> Long.parseLong(id)).collect(Collectors.toList());
            //查询一个
            LambdaQueryWrapper<UserReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(UserReport::getId, idList.get(0))
                    .in(UserReport::getHandleStatus, 1, 2);
            UserReport userReport = reportMapper.selectOne(reportWrapper);
            if (userReport != null) {
                return Result.fail("举报已处理,驳回失败");
            }
            ids.stream()
                    .forEach(id -> {
                        reportMapper.updatehandle(id, currentUserId, 2);
                    });
            return Result.ok("批量驳回成功");
        } catch (Exception e) {
            log.error("批量驳回失败", e);
            return Result.fail("操作失败");
        }

    }


}

