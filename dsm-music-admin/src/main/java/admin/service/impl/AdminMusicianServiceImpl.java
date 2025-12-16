package admin.service.impl;

import admin.domain.MusicianAdminEnum;
import admin.domain.dto.*;
import admin.domain.vo.*;
import admin.mapper.AdminMusicianMapper;
import admin.mapper.UserRole3Mapper;
import admin.service.AdminMusicianService;
import admin.service.UpdateUserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.mapper.UserMapper;
import entity.pojo.info.User;
import entity.pojo.info.UserRole;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static constants.RedisCacheConstants.TOKEN_USER_PREFIX;


@Service
@Slf4j

public class AdminMusicianServiceImpl implements AdminMusicianService {
    @Autowired
    private AdminMusicianMapper adminMusicianMapper;
    @Autowired
    private UserRole3Mapper userRole3Mapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UpdateUserInfoService updateUserInfoService;
    @Autowired
    private UserMapper userMapper;
    @Override
    public Result<PageResp<MusicianVO>> getMusicianList(MusiciansDTO musicianDTO) {
        //构建mybatis分页参数
        Page<MusicianVO> page = new Page<>(musicianDTO.getPageNum(), musicianDTO.getPageSize());
        IPage<MusicianVO> musicianList = adminMusicianMapper.getMusicianList(page, musicianDTO.getStatus(), musicianDTO.getKey());
        PageResp<MusicianVO> pageResp = new PageResp<>();
        pageResp.setRecords(musicianList.getRecords());
        pageResp.setTotal(musicianList.getTotal());
        pageResp.setPages(musicianList.getPages());
        pageResp.setCurrent(musicianList.getCurrent());
        pageResp.setSize(musicianList.getSize());
        return Result.ok(pageResp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result applyMusician(MusicianDTO musicianDTO) {
        try {
            //查询当前音乐人记录是否存在
            Musician musician = adminMusicianMapper.selectById(musicianDTO.getId());
            if(musician == null)
            {
                return Result.fail("当前审批记录不存在");
            }
            Integer status = musicianDTO.getStatus();
            if(MusicianAdminEnum.getDescByCode(status)==null)
            {
                return Result.fail("请输入正确的审核结果");
            }
            //变更审批状态
            UpdateWrapper<Musician> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", musicianDTO.getId())
                    .set("status", status);
            adminMusicianMapper.update(updateWrapper);
            //变更用户身份
            if(status==MusicianAdminEnum.APPROVED.getCode())
            {
                //把普通用户变成一个音乐人
                LambdaQueryWrapper<UserRole> lambdaQueryWrapper=new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(UserRole::getUserId,musician.getUserId())
                                .eq(UserRole::getRoleId,1);
                UserRole userRole1 = userRole3Mapper.selectOne(lambdaQueryWrapper);
                if(userRole1!=null) {
                    userRole1.setRoleId(3L);
                    userRole3Mapper.updateById(userRole1);
                    //变更redis中的权限
                    updateUserInfoService.updateUserInfo(musician.getUserId());
                }
            }
            return Result.ok("审批成功");
        }catch (Exception e)
        {
            log.error("审批失败",e);
            return Result.fail("审批失败");
        }
        }

    @Override
    public Result<String> batchPass(AdminMusicianApplyDTO musiciansDTO) {
        //批量通过
        List<String> idList = musiciansDTO.getApplyId();
        List<Long> ids = idList.stream().map(id->Long.parseLong(id)).collect(Collectors.toList());
        //变更审批状态
        adminMusicianMapper.batchPass(ids);
        //变更用户身份
        //把普通用户变成一个音乐人
        adminMusicianMapper.getListMusicianUserIds(ids).stream()
                .forEach(userId-> {
                    //把普通用户变成一个音乐人
                    LambdaQueryWrapper<UserRole> lambdaQueryWrapper=new LambdaQueryWrapper<>();
                    lambdaQueryWrapper.eq(UserRole::getUserId,userId)
                            .eq(UserRole::getRoleId,1);
                    UserRole userRole1 = userRole3Mapper.selectOne(lambdaQueryWrapper);
                    if(userRole1!=null) {
                        userRole1.setRoleId(3L);
                        userRole3Mapper.updateById(userRole1);
                        //变更redis中的权限
                        updateUserInfoService.updateUserInfo(userId);
                    }
                });
        return Result.ok("批量通过成功");
    }

    @Override
    public Result<String> batchRefuse(AdminMusicianApplyDTO musiciansDTO) {
        //批量驳回
        List<String> idList = musiciansDTO.getApplyId();
        List<Long> ids = idList.stream().map(id->Long.parseLong(id)).collect(Collectors.toList());
        adminMusicianMapper.batchRefuse(ids);
        return Result.ok("批量驳回成功");
    }

    @Override
    public Result<String> deleteMusicianApply(AdminMusicianApplyDTO adminMusicianApplyDTO) {
        List<String> applyId = adminMusicianApplyDTO.getApplyId();
        //查询一个
        LambdaQueryWrapper<Musician> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getId,Long.parseLong(applyId.get(0)))
                        .in(Musician::getStatus,1,2);
        Musician musician = adminMusicianMapper.selectOne(lambdaQueryWrapper);
        if(musician!=null) {
            return Result.fail("操作失败，仅限未处理的审核");
        }
        applyId.stream()
                .map(id->Long.parseLong(id))
                .forEach(id-> {
                   adminMusicianMapper.deleteById(id);
                });
        return Result.ok("删除成功");
    }
    //获取音乐人列表
    @Override
    public Result<PageResp<MusicianListVO>> getMusician(MusicianListDTO musicianListDTO) {
        Page<MusicianListVO> page = new Page<>(musicianListDTO.getPageNum(), musicianListDTO.getPageSize());
        IPage<MusicianListVO> musicianList = adminMusicianMapper.getMusician(page, musicianListDTO.getNameKey(), musicianListDTO.getStatus());
        PageResp<MusicianListVO> pageResp = new PageResp<>();
        pageResp.setRecords(musicianList.getRecords());
        pageResp.setTotal(musicianList.getTotal());
        pageResp.setPages(musicianList.getPages());
        pageResp.setCurrent(musicianList.getCurrent());
        pageResp.setSize(musicianList.getSize());
        return Result.ok(pageResp);
    }
    //封禁/解封音乐人
    @Override
    public Result<BanUserStatusVO> banMusician(String musicianId) {
        try {
            if (musicianId == null) {
                return Result.fail("音乐人ID不能为空");
            }
            Long Id = Long.valueOf(musicianId);
            //判断是不是音乐人
            LambdaQueryWrapper<Musician> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Musician::getId, Id)
                    .eq(Musician::getStatus, 1)
                    .in(Musician::getIsDeleted, 0, 1);
            Musician musician = adminMusicianMapper.selectOne(lambdaQueryWrapper);
            if (musician == null) {
                return Result.fail("音乐人不存在");
            }
            //根据状态封禁音乐人
            //判断音乐人状态
            Integer status = musician.getStatus();
            BanUserStatusVO banUserStatusVO = new BanUserStatusVO();
            //如果用户未封禁，则封禁
            if (status == 0) {
                musician.setIsDeleted(1);
                adminMusicianMapper.updateById(musician);
                banUserStatusVO.setBan(true);
                banUserStatusVO.setMsg("已封禁");
                Long userId = musician.getUserId();
                //变更用户身份
                UpdateWrapper<UserRole> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("user_id", userId)
                        .set("role_id", 1);
                userRole3Mapper.update(updateWrapper);
                //变更redis中的权限
                updateUserInfoService.updateUserInfo(userId);
                return Result.ok(banUserStatusVO);
            }
            //如果用户被封禁，则解封
            else if (status == 1) {
                LambdaQueryWrapper<User> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                lambdaQueryWrapper1.eq(User::getId, musician.getUserId())
                                .eq(User::getUserStatus,0);
                User user = userMapper.selectOne(lambdaQueryWrapper1);
                if(user!=null) {
                    return Result.fail("对应用户已封禁，音乐人解封失败");
                }
                musician.setIsDeleted(0);
                adminMusicianMapper.updateById(musician);
                banUserStatusVO.setBan(false);
                banUserStatusVO.setMsg("已解封");
                //变更用户身份
                UpdateWrapper<UserRole> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("user_id", musician.getUserId())
                        .set("role_id", 3);
                userRole3Mapper.update(updateWrapper);
                //变更redis中的权限
                updateUserInfoService.updateUserInfo(musician.getUserId());
                return Result.ok(banUserStatusVO);
            }
        } catch (Exception e) {
            log.error("封禁/解封音乐人失败", e);
            return Result.fail("操作失败");
        }
        return Result.fail("操作失败");
    }

    @Override
    public Result<String> deleteMusician(String musicianId) {
        //判断音乐人是否存在
        try {
            Long Id = Long.valueOf(musicianId);
            LambdaQueryWrapper<Musician> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Musician::getId, Id)
                    .eq(Musician::getStatus, 1)
                    .in(Musician::getIsDeleted, 0, 1);
            Musician musician = adminMusicianMapper.selectOne(lambdaQueryWrapper);
            if (musician == null) {
                return Result.fail("音乐人不存在");
            }
            //删除音乐人
            Long userId=musician.getUserId();
            musician.setIsDeleted(2);
            adminMusicianMapper.updateById(musician);
            //变更用户身份
            UpdateWrapper<UserRole> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_id", userId)
                    .set("role_id", 1);
            userRole3Mapper.update(updateWrapper);
            //变更redis中的权限
            updateUserInfoService.updateUserInfo(userId);
            return Result.ok("删除成功");
        }catch (Exception e)
        {
            log.error("删除音乐人失败", e);
            return Result.fail("删除失败");
        }
    }

    @Override
    public Result<String> deleteBatch(DeletedMusicianDTO deletedMusicianDTO) {
        try {
            List<String> MusicianIds = deletedMusicianDTO.getMusicianIds();
            //逐个删除用户
            MusicianIds.stream()
                    .map(Long::valueOf)
                    .forEach(musicianId -> {
                        LambdaQueryWrapper<Musician> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                        lambdaQueryWrapper.eq(Musician::getId, musicianId)
                                .eq(Musician::getStatus, 1)
                                .in(Musician::getIsDeleted, 0, 1);
                        Musician musician = adminMusicianMapper.selectOne(lambdaQueryWrapper);
                        if (musician != null) {
                            //删除音乐人,把音乐人的状态设置为注销
                            musician.setIsDeleted(2);
                            adminMusicianMapper.updateById(musician);
                            //变更用户身份
                            UpdateWrapper<UserRole> updateWrapper = new UpdateWrapper<>();
                            updateWrapper.eq("user_id", musician.getUserId())
                                    .set("role_id", 1);
                            userRole3Mapper.update(updateWrapper);
                            //变更redis中的权限
                            updateUserInfoService.updateUserInfo(musician.getUserId());
                        }
                    });
            return Result.ok("删除成功");
        }catch (Exception e){
            log.error("批量删除音乐人失败", e);
            return Result.fail("删除失败");
    }
    }

    @Override
    public Result<DetailMusicianVO> getDteailMusica(String id) {
        Long musicianId=Long.valueOf(id);
        //判断音乐人是否存在
        LambdaQueryWrapper<Musician> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getId, musicianId)
                .eq(Musician::getStatus, 1)
                .in(Musician::getIsDeleted, 0, 1);
        Musician musician = adminMusicianMapper.selectOne(lambdaQueryWrapper);
        if (musician == null) {
            return Result.fail("音乐人不存在");
        }
        //查询音乐人完整信息
        DetailMusicianVO detailMusicianVO = adminMusicianMapper.getDteailMusica(musician.getUserId());
        return Result.ok(detailMusicianVO);
    }
    //更新音乐人信息
    @Override
    public Result<String> updateMusicianInfo(UpdateMusicianDTO updateMusicianDTO) {
        LambdaQueryWrapper<Musician> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getId, updateMusicianDTO.getId())
                .eq(Musician::getStatus, 1)
                .in(Musician::getIsDeleted, 0, 1);
        Musician musician = adminMusicianMapper.selectOne(lambdaQueryWrapper);
        if (musician == null) {
            return Result.fail("音乐人不存在");
        }
        adminMusicianMapper.updateDetail(musician.getId());
        return Result.ok("更新成功");
    }
}
