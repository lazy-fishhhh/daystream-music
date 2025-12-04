package social.service.impl;

import cn.hutool.poi.excel.cell.CellSetter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.util.SecurityUtil;
import entity.pojo.social.CommunityPost;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.PostPhotos;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.saml2.Saml2SecurityMarker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import social.domain.LikeEnum;
import social.domain.dto.CommunityDTO;
import social.domain.dto.CommunityVOListDTO;
import social.domain.dto.MyPostDTO;
import social.domain.dto.UpdatePostDTO;
import social.domain.pojo.UserFollow;
import social.domain.vo.CommunityVO;
import social.domain.vo.PageLikeVO;
import social.domain.vo.PostDetailVO;
import social.mapper.CommunityPostMapper;
import social.mapper.FollowMapper;
import social.mapper.LikeRecord1Mapper;
import social.mapper.PostPhoneMapper;
import social.service.CommunityPostService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityPostServiceImpl implements CommunityPostService {
    @Autowired
    private CommunityPostMapper communityPostMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private LikeRecord1Mapper likeRecord1Mapper;
    @Autowired
    private PostPhoneMapper postPhoneMapper;
    //发布社区动态
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result releasePost(CommunityDTO communityDTO) {
        //获取当前用户的 id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        //创建动态
        CommunityPost communityPost=new CommunityPost();
        communityPost.setUserId(currentUserId);
        communityPost.setTitle(communityDTO.getTitle());
        communityPost.setContent(communityDTO.getContent());

        //保存动态
        int insert = communityPostMapper.insert(communityPost);
        if(insert>0){
            //判断是否有照片
            if(communityDTO.getImages()!=null&&!communityDTO.getImages().isEmpty())
            {
                //把照片存入数据库
                List<String> images=communityDTO.getImages();
                for(int i=0;i<images.size();i++)
                {
                    String image=images.get(i);
                    PostPhotos postPhotos=new PostPhotos();
                    postPhotos.setPhotoUrl(image);
                    postPhotos.setPostId(communityPost.getId());
                    postPhotos.setSortOrder(i);
                    postPhoneMapper.insert(postPhotos);
                }

            }
            return Result.ok("发布成功");
        }else
            return Result.fail("发布失败");
    }
    //删除动态
    @Override
    public Result deletePost(String postId) {
        //判断postId是否存在
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPost::getId,postId)
                .eq(CommunityPost::getIsDeleted,0);
        CommunityPost communityPost = communityPostMapper.selectOne(queryWrapper);
        if(communityPost==null){
            return Result.fail("动态不存在");
        }
        //判断当前用户是不是动态的作者
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if(!communityPost.getUserId().equals(currentUserId)){
            return Result.fail("没有权限删除");
        }
        //删除动态
        int delete = communityPostMapper.delete(queryWrapper);
        if(delete>0) {
            return Result.ok("删除成功");
        }
        else
            return Result.fail("删除失败");
        }
    //查询所有发布的动态
    @Override
    public Result getPost(CommunityVOListDTO communityVOListDTO) {
        Long lastId=null;
        if(communityVOListDTO.getLastId()!=null&&!communityVOListDTO.getLastId().equals(""))
        {
            lastId=Long.parseLong(communityVOListDTO.getLastId());
        }
        Integer page=communityVOListDTO.getPageSize();
        String key=communityVOListDTO.getKeyword();
        Long userId=communityVOListDTO.getUserId();
        Long currentUserId = SecurityUtil.getCurrentUserId();
        List<CommunityVO> communityVOS = communityPostMapper.selectCommunityPost(userId, page, lastId, key);
        if(communityVOS==null||communityVOS.isEmpty())
        {
            return Result.ok("未查询到数据");
        }
        List<CommunityVO> result = communityVOS.stream().map(communityVO -> {
                    //判断是否点赞
                    boolean like = communityPostMapper.isLike(currentUserId, Long.parseLong(communityVO.getPostId()));
                    communityVO.setIsLike(like);
                    //判断是否关注
                    boolean follow = communityPostMapper.isFollow(currentUserId, communityVO.getUserId());
                    communityVO.setIsFollow(follow);
                    //是否是vip
                    if(communityVO.getVipExpireTime()!=null)
                    {
                        if(LocalDateTime.now().isAfter(communityVO.getVipExpireTime()))
                        {
                            communityVO.setIsVip(false);
                        }else{
                            communityVO.setIsVip(true);
                        }
                    }
                    else{
                        communityVO.setIsVip(false);
                    }
                    //是否是我本人的动态
                    if(communityVO.getUserId().equals(currentUserId))
                    {
                        communityVO.setIsMine(true);
                    }
                    else{
                        communityVO.setIsMine(false);
                    }
                    //获取照片
                    List<String> photos = postPhoneMapper.selectPhote(Long.parseLong(communityVO.getPostId()));
                    communityVO.setPhotos(photos);
                    return communityVO;
                }
        ).collect(Collectors.toList());
        //获取本页最后一个id
        String currentId=communityVOS.get(result.size()-1).getPostId();
        Boolean hasMore=communityPostMapper.hasmore(userId,Long.parseLong(currentId),key);
        PageLikeVO<CommunityVO> pageLikeVO=new PageLikeVO<>();
        pageLikeVO.setDateList(result);
        pageLikeVO.setHasMore(hasMore);
        pageLikeVO.setLastId(currentId);
        return Result.ok(pageLikeVO);
    }
    //获取动态详情页
    @Override
    public Result getDetail(String postId1) {
        Long postId=Long.parseLong(postId1);
            //判断是否存在
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPost::getId,postId)
                .eq(CommunityPost::getIsDeleted,0);
        CommunityPost communityPost = communityPostMapper.selectOne(queryWrapper);
        if(communityPost==null){
            return Result.fail("动态不存在");
        }
        //存在就返回
        PostDetailVO detailVO=communityPostMapper.selectDetail(postId);
        //设置关注数
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserFollow::getUserId,communityPost.getUserId());
        Long count = followMapper.selectCount(lambdaQueryWrapper);
        detailVO.setFollowCount(count);
        //设置粉丝数
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper1=new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(UserFollow::getFollowedUserId,communityPost.getUserId());
        Long followerCount = followMapper.selectCount(lambdaQueryWrapper1);
        detailVO.setFollowerCount(followerCount);
        //是否关注作者
        lambdaQueryWrapper1.eq(UserFollow::getUserId,currentUserId)
                .eq(UserFollow::getFollowedUserId,detailVO.getAuthorId());
        UserFollow userFollow = followMapper.selectOne(lambdaQueryWrapper1);
        if(userFollow!=null)
        {
            detailVO.setIsFollow(true);
        }else {
            detailVO.setIsFollow(false);
        }

        //是否点赞
        LambdaQueryWrapper<LikeRecord> lambdaQueryWrapper2=new LambdaQueryWrapper<>();
        lambdaQueryWrapper2.eq(LikeRecord::getUserId,currentUserId)
                .eq(LikeRecord::getTargetId,postId)
                .eq(LikeRecord::getTargetType, LikeEnum.COMMUNITY.getValue())
                .eq(LikeRecord::getIsCanceled,0);
        LikeRecord likeRecord = likeRecord1Mapper.selectOne(lambdaQueryWrapper2);
        if(likeRecord!=null)
        {
            detailVO.setIsLike(true);
        }else {
            detailVO.setIsLike(false);
        }
        //是否是vip
        if(detailVO.getVipExpireTime()!=null)
        {
            if(LocalDateTime.now().isAfter(detailVO.getVipExpireTime()))
            {
                detailVO.setIsVip(false);
            }else{
                detailVO.setIsVip(true);
            }
        }
        else{
            detailVO.setIsVip(false);
        }
        //是否是我本人的动态
        if(currentUserId.equals(detailVO.getAuthorId()))
        {
            detailVO.setIsMine(true);
        }else {
            detailVO.setIsMine(false);
        }
        //动态照片
        List<String> photos = postPhoneMapper.selectPhote(postId);
        detailVO.setPhotos(photos);
        //查询作者的获赞数
        Long authorLikeCount= communityPostMapper.selectAllLikeCount(detailVO.getAuthorId());
        detailVO.setAuthorLikeCount(authorLikeCount);
        return Result.ok(detailVO);
        }
    @Transactional
    @Override
    //修改动态
    public Result updatePost(UpdatePostDTO updatePostDTO) {
        Long postId=Long.parseLong(updatePostDTO.getPostId());
        //判断是否存在
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPost::getId,postId)
                .eq(CommunityPost::getIsDeleted,0);
        CommunityPost communityPost = communityPostMapper.selectOne(queryWrapper);
        if(communityPost==null)
        {
            return Result.fail("动态不存在");
        }
        Long currentId=SecurityUtil.getCurrentUserId();
        if(!communityPost.getUserId().equals(currentId))
        {
            return Result.fail("您没有权限修改此动态");
        }
        //先修改标题跟内容
        communityPostMapper.updateTitleandContent(postId, updatePostDTO.getTitle(), updatePostDTO.getContent());
        //修改照片
        //删除旧照片
        postPhoneMapper.deleteByPostId(postId);
        //添加新照片
        List<String> photos = updatePostDTO.getImages();
        if(photos!=null&&photos.size()>0)
        {
            for(int i=0;i<photos.size();i++)
            {
                PostPhotos postPhotos=new PostPhotos();
                postPhotos.setPostId(postId);
                postPhotos.setPhotoUrl(photos.get(i));
                postPhotos.setSortOrder(i);
                postPhoneMapper.insert(postPhotos);
            }
        }
        return Result.ok("修改成功");
    }

    @Override
    public Result<PageResp<CommunityPost>> selectMypost(MyPostDTO myPostDTO) {
        //获取当前用户id
        Long currentId=SecurityUtil.getCurrentUserId();
        //构建分页
        Page<CommunityPost> page = new Page<>(myPostDTO.getPageNum(), myPostDTO.getPageSize());
        IPage<CommunityPost> communityPostIPage = communityPostMapper.selectMypost(currentId, page, myPostDTO.getStatus(), myPostDTO.getKeyword());
        PageResp<CommunityPost> pageResp = new PageResp<>();
        pageResp.setTotal(communityPostIPage.getTotal());
        pageResp.setCurrent(communityPostIPage.getCurrent());
        pageResp.setRecords(communityPostIPage.getRecords());
        pageResp.setPages(communityPostIPage.getPages());
        pageResp.setSize(communityPostIPage.getSize());
        return Result.ok(pageResp);

    }


}

