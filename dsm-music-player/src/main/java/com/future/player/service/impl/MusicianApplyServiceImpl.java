package com.future.player.service.impl;

import admin.service.UpdateUserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.mapper.UserRoleMapper;
import com.future.login.util.SecurityUtil;
import com.future.player.entity.dto.DisableAccountDTO;
import com.future.player.entity.dto.MusicianApplyDTO;
import com.future.player.entity.dto.MusicianListDTO;
import com.future.player.entity.dto.UpdateMusicianDTO;
import com.future.player.mapper.PlayerMusicianMapper;
import com.future.player.service.MusicianApplyService;
import constants.RegexPatterns;
import entity.pojo.info.User;
import entity.pojo.info.UserRole;
import entity.pojo.music.Musician;
import entity.pojo.music.SingerTagRelation;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.RoleEnum;
import com.future.player.entity.vo.MusicianInfoVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static constants.RedisCacheConstants.USER_EMAILCAPTCHA;

@Service
public class MusicianApplyServiceImpl implements MusicianApplyService {
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private UpdateUserInfoService updateUserInfoService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PlayerMusicianMapper playerMusicianMapper;
    //判断用户是不是音乐人
    @Override
    public Result<Boolean> isMusician() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserRole> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserRole::getUserId,currentUserId)
                .eq(UserRole::getRoleId, RoleEnum.MUSICIAN.getValue());
        UserRole userRole = userRoleMapper.selectOne(lambdaQueryWrapper);
        if(userRole!=null)
        {
            return Result.ok(true);
        }
        return Result.ok(false);
    }

    @Override
    public Result<String> cancelApply() {
        // 获取当前用户的id
        Long userId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Musician> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getUserId, userId)
                .eq(Musician::getStatus, 0);
        Musician musician = playerMusicianMapper.selectOne(lambdaQueryWrapper);
        if (musician == null) {
            return Result.fail("未找到待审核的申请记录");
        }
        // 执行撤销操作
        int rows = playerMusicianMapper.deleteById(musician.getId());
        if (rows > 0) {
            return Result.ok("申请已成功撤销");
        } else {
            return Result.fail("撤销失败，请重试");
        }
    }

    @Override
    public Result<String> deleteMusician(DisableAccountDTO disableAccountDTO) {

        //获取当前用户的信息
        User user=SecurityUtil.getCurrentUser();
        if(user==null)
        {
            return Result.fail("用户不存在");
        }
        Long userId = user.getId();
        LambdaQueryWrapper<Musician> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getUserId,userId)
                .eq(Musician::getStatus,1);
        Musician musician = playerMusicianMapper.selectOne(lambdaQueryWrapper);
        if(musician==null)
        {
            return Result.fail("您还不是音乐人，无法注销音乐人账号");
        }
        //判断个人信息是否正确
        String email = disableAccountDTO.getEmail();
        if (!RegexPatterns.matches(email, RegexPatterns.EMAIL_REGEX)) {
            return Result.fail("邮箱格式不正确，请输入正确的邮箱");
        }
        String email1=user.getEmail();
        if(!Objects.equals(email1,email))
        {
            return Result.fail("邮箱与当前用户不匹配");
        }
        //判断密码是否相等
        String password = disableAccountDTO.getPassword();
        if(!bCryptPasswordEncoder.matches(password,user.getPassword()))
        {
            return Result.fail("密码错误");
        }
        //判断邮箱验证码是否正确
        String captcha = disableAccountDTO.getCaptcha();
        //校验验证码
        String key=USER_EMAILCAPTCHA+email;
        String captcha1 = stringRedisTemplate.opsForValue().get(key);
        if(Objects.isNull(captcha))
        {
            return Result.fail("验证码已过期，请重新获取");
        }
        if(!Objects.equals(captcha,captcha1))
        {
            return Result.fail("请输入正确的验证码");
        }
        //注销
        playerMusicianMapper.deleteMusician(musician.getId());
        //更新角色，从音乐人变成普通用户
        UpdateWrapper<UserRole> updateWrapper1=new UpdateWrapper<>();
        updateWrapper1.eq("user_id",userId)
                .eq("role_id",RoleEnum.MUSICIAN.getValue())
                .set("role_id",RoleEnum.USER.getValue());
        userRoleMapper.update(null,updateWrapper1);
        //变更reids中的权限
        updateUserInfoService.updateUserInfo(userId);
       return Result.ok("注销成功");
    }

    @Override
    public Result<MusicianInfoVO> getMuscianInfo() {
        Long currentUserId=SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Musician> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getUserId,currentUserId)
                .eq(Musician::getStatus,1);
        Musician musician = playerMusicianMapper.selectOne(lambdaQueryWrapper);
        if(musician==null)
        {
            return Result.fail("您还不是音乐人");
        }
        MusicianInfoVO musicianInfoVO = playerMusicianMapper.selectMusicianInfo(currentUserId);
        //设置标签
        List<Long> tags = playerMusicianMapper.selectMusicianTag(Long.parseLong(musicianInfoVO.getId()));
        musicianInfoVO.setTags(tags);
        return Result.ok(musicianInfoVO);

    }

    @Override
    public Result<PageResp<MusicianInfoVO>> getAllMusician(MusicianListDTO musicianListDTO) {
        //构建分页
        Page<MusicianInfoVO> page = new Page<>(musicianListDTO.getPageNum(), musicianListDTO.getPageSize());
        IPage<MusicianInfoVO> musicianInfoVOS = playerMusicianMapper.searchMusicians(musicianListDTO.getName(), musicianListDTO.getTagIds(),page);
        List<MusicianInfoVO> musicianInfoVOList = musicianInfoVOS.getRecords();
        if(musicianInfoVOList!=null&&musicianInfoVOList.size()>0) {
            musicianInfoVOList.stream()
                    .forEach(musicianInfoVO -> musicianInfoVO.setTags(playerMusicianMapper.selectMusicianTag(Long.parseLong(musicianInfoVO.getId()))));
        }
        PageResp resp=new PageResp();
        resp.setRecords(musicianInfoVOS.getRecords());
        resp.setTotal(musicianInfoVOS.getTotal());
        resp.setSize(musicianInfoVOS.getSize());
        resp.setPages(musicianInfoVOS.getPages());
        resp.setCurrent(musicianInfoVOS.getCurrent());
        return Result.ok(resp);



    }

    @Override
    public Result<String> updateMusicianInfo(UpdateMusicianDTO updateMusicianDTO) {
        Long id = Long.parseLong(updateMusicianDTO.getId());
        playerMusicianMapper.updateMusicianInfo(updateMusicianDTO);
        return Result.ok("修改成功");
    }


    @Override
    @Transactional
    public Result applyMusician(MusicianApplyDTO musicianApplyDTO) {

        Long currentUserId=SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<Musician> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Musician::getUserId,currentUserId)
                .eq(Musician::getStatus,0);
        Musician musician = playerMusicianMapper.selectOne(lambdaQueryWrapper);
        if(musician!=null) {
            return Result.fail("您已经申请过申请，请耐心等待认证");
        }
        LambdaQueryWrapper<Musician> lambdaQueryWrapper1=new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(Musician::getUserId,currentUserId)
                .eq(Musician::getStatus,1)
                .eq(Musician::getIsDeleted,0);
        Musician musician1 = playerMusicianMapper.selectOne(lambdaQueryWrapper1);
        if(musician1!=null) {
            return Result.fail("您已经入驻过音乐人，请勿重复申请");
        }
        //判断是否申请过
        String username = musicianApplyDTO.getStageName();
        if (!RegexPatterns.matches(username, RegexPatterns.USERNAME_REGEX)) {
            return Result.fail("用户名2-16位，请输入正确的用户名");
        }
        //判断用户名是否合法
        //如果申请过被驳回，则重新改成待审核
        LambdaQueryWrapper<Musician> lambdaQueryWrapper2=new LambdaQueryWrapper<>();
        lambdaQueryWrapper2.eq(Musician::getUserId,currentUserId)
                .eq(Musician::getStatus,2);
        Musician musician2 = playerMusicianMapper.selectOne(lambdaQueryWrapper2);
        if(musician2!=null)
        {
            musician2.setStatus(0);
            musician2.setCreatedTime(LocalDateTime.now());
            musician2.setStageName(musicianApplyDTO.getStageName());
            musician2.setIntroduction(musicianApplyDTO.getIntroduction());
            musician2.setAvatar(musicianApplyDTO.getAvatar());
            playerMusicianMapper.update(musician2,lambdaQueryWrapper2);
            return Result.ok("申请成功，请等待审核");
        }
        //如果之前是音乐人，现在注销了，把审核状态变成0，是否注销变成0
        LambdaQueryWrapper<Musician> lambdaQueryWrapper3=new LambdaQueryWrapper<>();
        lambdaQueryWrapper3.eq(Musician::getUserId,currentUserId)
                .eq(Musician::getStatus,1)
                .eq(Musician::getIsDeleted,1);
        Musician musician4=playerMusicianMapper.selectOne(lambdaQueryWrapper3);
        if(musician4!=null)
        {
            musician4.setStatus(0);
            musician4.setIsDeleted(0);
            musician2.setCreatedTime(LocalDateTime.now());
            musician2.setStageName(musicianApplyDTO.getStageName());
            musician2.setIntroduction(musicianApplyDTO.getIntroduction());
            musician2.setAvatar(musicianApplyDTO.getAvatar());
            playerMusicianMapper.update(musician4,lambdaQueryWrapper3);
        }
        //创建音乐人申请
        Musician musician3=new Musician();
        musician3.setUserId(currentUserId);
        musician3.setStageName(musicianApplyDTO.getStageName());
        musician3.setAvatar(musicianApplyDTO.getAvatar());
        List<Integer> tags = musicianApplyDTO.getTags();
        int insert = playerMusicianMapper.insert(musician3);
        //添加标签到音乐人与标签关联表
        tags.stream()
                .forEach(tagId -> {
                    SingerTagRelation musicianTag = new SingerTagRelation();
                    musicianTag.setMusicianId(musician3.getId());
                    musicianTag.setTagId(tagId);
                    playerMusicianMapper.insertTag(musicianTag);
                });
        if(insert>0)
        {
            return Result.ok("申请成功，请等待审核");
        }
        return Result.fail("申请失败");
    }

}
