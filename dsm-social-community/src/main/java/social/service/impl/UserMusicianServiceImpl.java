package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.info.UserRole;
import entity.pojo.music.Musician;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import social.domain.RoleEnum;
import social.domain.dto.MusicianApplyDTO;
import social.domain.vo.UserInfoVO;
import social.mapper.FollowMapper;
import social.mapper.Musician2Mapper;
import social.mapper.User2Mapper;
import social.mapper.User2RoleMapper;
import social.service.UserMusicianService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserMusicianServiceImpl implements UserMusicianService {
    @Autowired
    private Musician2Mapper musicianMapper;
    @Autowired
    private User2Mapper user2Mapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private User2RoleMapper userRoleMapper;
    @Resource
    private ObjectMapper objectMapper;
        @Override
        public Result getUserInfo() {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            UserInfoVO  userInfoVO=user2Mapper.userInfo(currentUserId);
            if(userInfoVO==null)
            {
                return Result.fail("用户不存在");
            }
            //判断是否是vip
            if(userInfoVO.getVipExpireTime()!=null) {
                //判断是否是vip
                if (LocalDateTime.now().isAfter(userInfoVO.getVipExpireTime())) {
                    userInfoVO.setIsVip(false);
                }
                userInfoVO.setIsVip(true);
            }
            else {
                userInfoVO.setIsVip(false);
            }
            //判断用户身份
            List<Integer> roleIds = user2Mapper.roleIds(currentUserId);
            List<String> docs = RoleEnum.docs(roleIds);
            userInfoVO.setUserRole(docs);
            return Result.ok(userInfoVO);
        }



    //判断用户是不是音乐人
    @Override
    public Result isMusician() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserRole> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserRole::getUserId,currentUserId);
        UserRole userRole = userRoleMapper.selectOne(lambdaQueryWrapper);
        if(userRole!=null)
        {
            return Result.ok(true);
        }
        return Result.ok(false);
    }
    @Override
    public Result applyMusician(MusicianApplyDTO musicianApplyDTO) {

            Long currentUserId=SecurityUtil.getCurrentUserId();
            LambdaQueryWrapper<Musician> lambdaQueryWrapper=new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Musician::getUserId,currentUserId)
                    .eq(Musician::getStatus,0);
            Musician musician = musicianMapper.selectOne(lambdaQueryWrapper);
            if(musician!=null) {
                return Result.fail("您已经申请过申请，请耐心等待认证");
            }
            //创建音乐人申请
            Musician musician1=new Musician();
            musician1.setUserId(currentUserId);
            musician1.setStageName(musicianApplyDTO.getStageName());
            musician1.setAvatar(musicianApplyDTO.getAvatar());
            int insert = musicianMapper.insert(musician1);
            if(insert>0)
            {
                return Result.ok("申请成功，请等待审核");
            }
            return Result.fail("申请失败");
    }

}
