package social.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.info.User;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import social.domain.RoleEnum;
import social.domain.pojo.UserFollow;
import social.domain.vo.OtherUserVO;
import social.domain.vo.UserInfoVO;
import social.mapper.FollowMapper;
import social.mapper.User2Mapper;
import social.service.UserInfoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static constants.RedisCacheConstants.USER_INFO_PREFIX;
@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    private User2Mapper user2Mapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FollowMapper followMapper;
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

    @Override
    public Result getOtherUserInfo(Long userId) {
            if(userId==null)
            {
                return Result.fail("用户id不能为空");
            }
        OtherUserVO otherUserVO = user2Mapper.otherUserInfo(userId);
        if(otherUserVO==null)
        {
            return Result.fail("用户不存在");
        }
        Long currentUserId = SecurityUtil.getCurrentUserId();
        //关注数
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserFollow::getUserId,userId);
        Long count = followMapper.selectCount(lambdaQueryWrapper);
        otherUserVO.setFollowCount(count);
        //设置粉丝数
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper1=new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(UserFollow::getFollowedUserId,userId);
        Long followerCount = followMapper.selectCount(lambdaQueryWrapper1);
        otherUserVO.setFollowerCount(followerCount);
        //是否关注作者
        lambdaQueryWrapper1.eq(UserFollow::getUserId,currentUserId)
                .eq(UserFollow::getFollowedUserId,userId);
        UserFollow userFollow = followMapper.selectOne(lambdaQueryWrapper1);
        if(userFollow!=null)
        {
            otherUserVO.setIsFollow(true);
        }else {
            otherUserVO.setIsFollow(false);
        }
        //是否是vip
        if(otherUserVO.getVipExpireTime()!=null) {
            //判断是否是vip
            if (LocalDateTime.now().isAfter(otherUserVO.getVipExpireTime())) {
                otherUserVO.setIsVip(false);
            }
            otherUserVO.setIsVip(true);
        }
        else {
            otherUserVO.setIsVip(false);
        }
        return Result.ok(otherUserVO);
    }

}
