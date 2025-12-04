package admin.service.impl;

import admin.domain.MusicianAdminEnum;
import admin.domain.dto.MusicianDTO;
import admin.domain.dto.MusiciansDTO;
import admin.domain.vo.MusicianVO;
import admin.mapper.AdminMusicianMapper;
import admin.mapper.UserRole3Mapper;
import admin.service.AdminMusicianService;
import admin.service.UpdateUserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.User;
import entity.pojo.info.UserRole;
import entity.pojo.music.Musician;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
                UpdateWrapper<UserRole> queryWrapper = new UpdateWrapper<>();
                queryWrapper.eq("user_id", musician.getUserId())
                        .set("role_id", 3);
                userRole3Mapper.update(queryWrapper);
                //变更redis中的权限
                updateUserInfoService.updateUserInfo(musician.getUserId());
            }
            return Result.ok("审批成功");
        }catch (Exception e)
        {
            return Result.fail("审批失败");
        }
        }

}
