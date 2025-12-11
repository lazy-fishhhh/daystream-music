package social.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.exception.BusinessException;
import com.future.login.mapper.UserRoleMapper;
import com.future.login.util.SecurityUtil;
import constants.RedisCacheConstants;
import entity.pojo.info.User;
import entity.pojo.info.UserRole;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.music.PlaylistMusic;
import entity.pojo.social.*;
import entity.pojo.util.Result;
import io.lettuce.core.RedisClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import social.domain.CollectionEnum;
import social.domain.LikeEnum;
import social.domain.RoleEnum;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.dto.PageDTO;
import social.domain.pojo.UserFollow;
import social.domain.vo.*;
import social.mapper.*;
import social.service.UserBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static constants.RedisCacheConstants.*;

/**
 * 用户基础社交功能 点赞/收藏/关注/评论
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBaseServiceImpl implements UserBaseService {
    private final CommunityPlaylistMapper communityPlaylistMapper;

    @Autowired
    private LikeRecord1Mapper likeRecordMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private Music2Mapper music2Mapper;
    @Autowired
    private PlayList2Mapper playList2Mapper;
    @Autowired
    private UserCollection2Mapper userCollection2Mapper;
    @Autowired
    private Album2Mapper album2Mapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private User2Mapper user2Mapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private CommunityPostMapper communityPostMapper;
    //点赞/取消点赞功能
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<LikeVO> likeandUnlike(LikeDTO likeDTO) {
        //获取用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long targetId = Long.parseLong(likeDTO.getTargetId());
        Integer targetType = likeDTO.getTargetType();
        if (Objects.isNull(targetId)) {
            return Result.fail("目标id不能为空");
        }
        //判断点赞类型是否合理
        LikeEnum likeEnumByvalue = LikeEnum.getLikeEnumByvalue(targetType);
        if (likeEnumByvalue == null) {
            return Result.fail("请输入正确的点赞类型");
        }
        //判断目标id的数据是否存在 目标类型 1-音乐 2-评论 3-动态
        if (targetType == LikeEnum.MUSIC.getValue()) {
            LambdaQueryWrapper<Music> musicWrapper = new LambdaQueryWrapper();
            musicWrapper.eq(Music::getId, targetId)
                    .eq(Music::getStatus, 1);
            Music music = music2Mapper.selectOne(musicWrapper);
            if (music == null) {
                return Result.fail("音乐暂未上架");
            }
        } else if (targetType == LikeEnum.COMMENT.getValue()) {
            LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper();
            commentWrapper.eq(Comment::getId, targetId)
                    .eq(Comment::getStatus, 1);
            Comment comment = commentMapper.selectOne(commentWrapper);
            if (comment == null) {
                return Result.fail("评论不存在");
            }
        }
        else if(targetType==LikeEnum.COMMUNITY.getValue())
        {
            LambdaQueryWrapper<CommunityPost> lambdaQueryWrapper=new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(CommunityPost::getId,targetId)
                    .eq(CommunityPost::getIsDeleted,0);
            CommunityPost communityPost = communityPostMapper.selectOne(lambdaQueryWrapper);
            if(communityPost==null)
            {
                return Result.fail("动态不存在");
            }
        }
        //根据点赞类型不同，处理不同的点赞
        //如果点赞记录已存在只是之前逻辑取消点赞了,那么重新恢复点赞记录
        try {
            LambdaQueryWrapper<LikeRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(LikeRecord::getUserId, currentUserId)
                    .eq(LikeRecord::getTargetId, targetId)
                    .eq(LikeRecord::getTargetType, targetType);
            LikeRecord likeRecord = likeRecordMapper.selectOne(lambdaQueryWrapper);
            LikeVO likevo = new LikeVO();
            String key=USER_LIKE+":"+currentUserId;
            //如果有点赞记录，判断是否点赞过，如果之前取消了则恢复
            if (likeRecord != null) {
                //如果点赞过了，则取消
                if (likeRecord.getIsCanceled() == 0) {
                    //取消
                    likeRecord.setIsCanceled(1);
                    likeRecord.setUpdatedTime(LocalDateTime.now());
                    likeRecordMapper.updateById(likeRecord);
                    Long count = updateLikeCount(true, targetId, targetType);
                    likevo.setIslike(false);
                    likevo.setMsg("取消点赞成功");
                    likevo.setLikecount(count);
                    if(targetType==LikeEnum.MUSIC.getValue())
                    {
                        LikeMusicVO likeMusicVO = music2Mapper.selectLikeMusicVO(targetId,currentUserId);
                        stringRedisTemplate.opsForZSet().remove(key,objectMapper.writeValueAsString(likeMusicVO));
                    }
                    //  在"我的喜欢"歌单中删除这首歌
                    LambdaQueryWrapper<Playlist> playlistWrapper = new LambdaQueryWrapper<>();
                    playlistWrapper.eq(Playlist::getUserId,currentUserId)
                            .eq(Playlist::getPlaylistName,"我的喜欢");
                    Playlist playlist = playList2Mapper.selectOne(playlistWrapper);
                    if(playlist == null)
                    {
                        throw new BusinessException("10001","歌单'我的喜欢'不存在");
                    }

                    LambdaQueryWrapper<PlaylistMusic> communityPlaylistWrapper = new LambdaQueryWrapper<>();
                    communityPlaylistWrapper.eq(PlaylistMusic::getPlaylistId,playlist.getId())
                                            .eq(PlaylistMusic::getMusicId,targetId);
                    communityPlaylistMapper.delete(communityPlaylistWrapper);
                    log.info("删除我的喜欢歌曲成功");
                    return Result.ok(likevo);
                }
                //如果此前取消了,现在要恢复
                if (likeRecord.getIsCanceled() == 1) {
                    //恢复
                    likeRecord.setIsCanceled(0);
                    likeRecord.setUpdatedTime(LocalDateTime.now());
                    likeRecordMapper.updateById(likeRecord);
                    Long count = updateLikeCount(false, targetId, targetType);
                    likevo.setIslike(true);
                    likevo.setMsg("点赞成功");
                    likevo.setLikecount(count);
                    if(targetType==LikeEnum.MUSIC.getValue())
                    {
                        //创建喜欢的音乐视图
                        LikeMusicVO likeMusicVO = music2Mapper.selectLikeMusicVO(targetId,currentUserId);
                        Long epochSecond=likeMusicVO.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                        stringRedisTemplate.expire(key, 7, TimeUnit.DAYS); // 先设过期
                        Boolean addResult = stringRedisTemplate.opsForZSet().add(key, objectMapper.writeValueAsString(likeMusicVO), epochSecond);
                    }
                    //  在"我的喜欢"歌单中添加这首歌
                    LambdaQueryWrapper<Playlist> playlistWrapper = new LambdaQueryWrapper<>();
                    playlistWrapper.eq(Playlist::getUserId,currentUserId)
                            .eq(Playlist::getPlaylistName,"我的喜欢");
                    Playlist playlist = playList2Mapper.selectOne(playlistWrapper);
                    if(playlist == null)
                    {
                        throw new BusinessException("10001","歌单'我的喜欢'不存在");
                    }
                    PlaylistMusic playlistMusic = new PlaylistMusic();
                    playlistMusic.setPlaylistId(playlist.getId());
                    playlistMusic.setMusicId(targetId);
                    communityPlaylistMapper.insert(playlistMusic);
                    log.info("添加我的喜欢歌曲成功");
                    return Result.ok(likevo);
                }
            }
            //如果没有点赞过，创建新的点赞记录
            LikeRecord likeRecord1 = new LikeRecord();
            likeRecord1.setUserId(currentUserId);
            likeRecord1.setTargetId(targetId);
            likeRecord1.setTargetType(targetType);
            likeRecordMapper.insert(likeRecord1);
            Long count = updateLikeCount(false, targetId, targetType);
            likevo.setIslike(true);
            likevo.setLikecount(count);
            likevo.setMsg("点赞成功");
            if(targetType==LikeEnum.MUSIC.getValue())
            {
                //创建喜欢的音乐视图
                LikeMusicVO likeMusicVO = music2Mapper.selectLikeMusicVO(targetId,currentUserId);
                Long epochSecond=likeMusicVO.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                stringRedisTemplate.opsForZSet().add(key,objectMapper.writeValueAsString(likeMusicVO),epochSecond);
                stringRedisTemplate.expire(key,7, TimeUnit.DAYS);
            }
            // 在"我的喜欢"歌单中添加这首歌
            LambdaQueryWrapper<Playlist> playlistWrapper = new LambdaQueryWrapper<>();
            playlistWrapper.eq(Playlist::getUserId,currentUserId)
                    .eq(Playlist::getPlaylistName,"我的喜欢");
            Playlist playlist = playList2Mapper.selectOne(playlistWrapper);
            if(playlist == null)
            {
                throw new BusinessException("10001","歌单'我的喜欢'不存在");
            }
            PlaylistMusic playlistMusic = new PlaylistMusic();
            playlistMusic.setPlaylistId(playlist.getId());
            playlistMusic.setMusicId(targetId);
            communityPlaylistMapper.insert(playlistMusic);
            log.info("添加我的喜欢歌曲成功");
            return Result.ok(likevo);
        } catch (DuplicateKeyException e) {
            log.error("并发错误", e);
            return Result.fail("点赞失败");
        } catch (Exception e) {
            log.error("运行错误", e);
            return Result.fail("点赞失败");
        }
    }


    //变更点赞数
    private Long updateLikeCount(Boolean isCancle, Long targetId, Integer targetType) {

        //变更对应目标的点赞量
        long count = 0;
        if (targetType == LikeEnum.MUSIC.getValue()) {
            count = music2Mapper.selectCount(targetId);
            if (isCancle) {
                if(count>0) {
                    music2Mapper.likeCountLow(targetId);
                    count--;
                }
            } else {
               music2Mapper.likeCountUp(targetId);
               count++;
            }
            return count;
        } else if (targetType == LikeEnum.COMMENT.getValue()) {
            count = commentMapper.selectCount(targetId);
            if (isCancle) {
                if (count > 0) {
                    commentMapper.updateLikeCountLow(targetId);
                    count--;
                }
            } else {
                commentMapper.updateLikeCountup(targetId);
                count++;
            }
        }else if(targetType==LikeEnum.COMMUNITY.getValue())
            {
                count=communityPostMapper.selectCount(targetId);
                if(isCancle)
                {
                    if (count>0)
                    {
                        communityPostMapper.updateLikeCountLow(targetId);
                        count--;
                    }
                 }else{
                    communityPostMapper.updateLikeCountup(targetId);
                    count++;
                }
        }
        return  count;
    }



    /**
     * 收藏
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<CollectionVO> collectUnCollect(CollectionDTO collectionDTO) {
        //获取用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long targetId = Long.parseLong(collectionDTO.getTargetId());
        Integer targetType = collectionDTO.getTargetType();
        if (Objects.isNull(targetId)) {
            return Result.fail("目标id不能为空");
        }
        //判断收藏类型是否合理
        CollectionEnum collectionEnumByvalue = CollectionEnum.getCollectByValue(targetType);
        if (collectionEnumByvalue == null) {
            return Result.fail("请输入正确的点赞类型");
        }
        //判断目标id的数据是否存在 目标类型 1-歌单 2-专辑
        if (targetType == CollectionEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> playListWrapper = new LambdaQueryWrapper();
            playListWrapper.eq(Playlist::getId, targetId);
            Playlist playlist = playList2Mapper.selectOne(playListWrapper);
            if (playlist == null) {
                return Result.fail("歌单不存在");
            }
            //如果歌单私有
            else if (playlist.getIsPublic() == 0) {
                if (playlist.getUserId() != currentUserId) {
                    return Result.fail("该歌单未公开，无权收藏");
                }
            }
        } else if (targetType == CollectionEnum.AlBUM.getValue()) {
            LambdaQueryWrapper<Album> albumLambdaQueryWrapper = new LambdaQueryWrapper<>();
            albumLambdaQueryWrapper.eq(Album::getId, targetId)
                    .eq(Album::getStatus, 1);
            Album album = album2Mapper.selectOne(albumLambdaQueryWrapper);
            if (album == null) {
                return Result.fail("该专辑未上架");
            }
        }
        try {
            //根据收藏类型不同，处理不同的收藏
            //如果收藏记录已存在只是之前逻辑取消收藏了,那么重新恢复收藏记录
            LambdaQueryWrapper<UserCollection> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(UserCollection::getUserId, currentUserId)
                    .eq(UserCollection::getTargetId, targetId)
                    .eq(UserCollection::getTargetType, targetType);
            UserCollection userCollection = userCollection2Mapper.selectOne(lambdaQueryWrapper);
            CollectionVO collectionVO = new CollectionVO();
            //如果有收藏记录，判断是否收藏过，如果之前取消了则恢复
            if (userCollection != null) {
                //如果收藏过了，则取消
                if (userCollection.getIsCanceled() == 0) {
                    //取消
                    userCollection.setIsCanceled(1);
                    userCollection.setUpdatedTime(LocalDateTime.now());
                    userCollection2Mapper.updateById(userCollection);
                    Long count = updateCollectCount(true, targetId, targetType);
                    collectionVO.setIsCollection(false);
                    collectionVO.setMsg("取消收藏成功");
                    collectionVO.setCollectCount(count);
                    if(targetType==CollectionEnum.PLAYLIST.getValue())
                    {
                        String key=USER_COLLECTION+":"+"playList"+currentUserId;
                        CollectonLIstVO collectonLIstVO=playList2Mapper.selectPlayListVO(targetId,currentUserId);
                        stringRedisTemplate.opsForZSet().remove(key,objectMapper.writeValueAsString(collectonLIstVO));
                    }
                    else if(targetType==CollectionEnum.AlBUM.getValue())
                    {
                        String key=USER_COLLECTION+":"+"album"+currentUserId;
                        CollectioAlbumVO collectioAlbumVO=album2Mapper.selectAlbumList(targetId,currentUserId);
                        stringRedisTemplate.opsForZSet().remove(key,objectMapper.writeValueAsString(collectioAlbumVO));
                    }
                    return Result.ok(collectionVO);
                }
                //如果此前取消了,现在要恢复
                if (userCollection.getIsCanceled() == 1) {
                    //恢复
                    userCollection.setIsCanceled(0);
                    userCollection.setUpdatedTime(LocalDateTime.now());
                    userCollection2Mapper.updateById(userCollection);
                    Long count = updateCollectCount(false, targetId, targetType);
                    collectionVO.setIsCollection(true);
                    collectionVO.setMsg("收藏成功");
                    collectionVO.setCollectCount(count);
                    if(targetType==CollectionEnum.PLAYLIST.getValue())
                    {
                        String key=USER_COLLECTION+":"+"playList"+currentUserId;
                        Long epochSecond = userCollection.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                        CollectonLIstVO collectonLIstVO=playList2Mapper.selectPlayListVO(targetId,currentUserId);
                        stringRedisTemplate.opsForZSet().add(key,objectMapper.writeValueAsString(collectonLIstVO),epochSecond);
                        stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
                    }
                    else if(targetType==CollectionEnum.AlBUM.getValue())
                    {
                        String key=USER_COLLECTION+":"+"album"+currentUserId;
                        Long epochSecond = userCollection.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                        CollectioAlbumVO collectioAlbumVO=album2Mapper.selectAlbumList(targetId,currentUserId);
                        stringRedisTemplate.opsForZSet().add(key,objectMapper.writeValueAsString(collectioAlbumVO),epochSecond);
                        stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
                    }
                    return Result.ok(collectionVO);
                }
            }
            //如果没有收藏过，创建新的收藏记录
            UserCollection userCollection1 = new UserCollection();
            userCollection1.setUserId(currentUserId);
            userCollection1.setTargetId(targetId);
            userCollection1.setTargetType(targetType);
            userCollection2Mapper.insert(userCollection1);
            Long count = updateCollectCount(false, targetId, targetType);
            collectionVO.setIsCollection(true);
            collectionVO.setCollectCount(count);
            collectionVO.setMsg("收藏成功");
            if(targetType==CollectionEnum.PLAYLIST.getValue())
            {
                String key=USER_COLLECTION+":"+"playList"+currentUserId;
                Long epochSecond = userCollection.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                CollectonLIstVO collectonLIstVO=playList2Mapper.selectPlayListVO(targetId,currentUserId);
                stringRedisTemplate.opsForZSet().add(key,objectMapper.writeValueAsString(collectonLIstVO),epochSecond);
                stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
            }
            else if(targetType==CollectionEnum.AlBUM.getValue())
            {
                String key=USER_COLLECTION+":"+"album"+currentUserId;
                Long epochSecond = userCollection.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                CollectioAlbumVO collectioAlbumVO=album2Mapper.selectAlbumList(targetId,currentUserId);
                stringRedisTemplate.opsForZSet().add(key,objectMapper.writeValueAsString(collectioAlbumVO),epochSecond);
                stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
            }
            return Result.ok(collectionVO);
        } catch (DuplicateKeyException e) {
            log.error("并发错误", e);
            return Result.fail("收藏失败");
        } catch (Exception e) {
            log.error("运行错误", e);
            return Result.fail("收藏失败");
        }
        }

    //变更收藏数
    private Long updateCollectCount(Boolean isCancle,Long targetId,Integer targetType)
    {

        //变更对应目标的收藏量
        long count=0;
        if(targetType==CollectionEnum.PLAYLIST.getValue()) {
            count= playList2Mapper.selectCount(targetId);
            if(isCancle) {
                if(count>0) {
                    playList2Mapper.playlistCollectCountLow(targetId);
                    count--;
                }
            }else {

                playList2Mapper.playlistCollectCountup(targetId);
                count++;
            }
            return count;
        }else if(targetType==CollectionEnum.AlBUM.getValue())
        {
            count=album2Mapper.selectCount(targetId);
            if(isCancle)
            {
                if(count>0)
                {
                    album2Mapper.albumCollectCountLow(targetId);
                    count--;
                }
            }
            else{
                album2Mapper.albumCollectCountup(targetId);
                count++;
            }
            return count;
        }
        return count;
    }


    //获取用户的点赞列表
    @Override
    public Result getLikeMusic(PageDTO pageDTO)
    {
        //获取当前用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Integer pageSize=pageDTO.getSize();
        //查询reids中的数据
        String key=USER_LIKE+":"+currentUserId;
        List<LikeMusicVO> musicVOS=new ArrayList<>();
        Long start=0L;
        Long end=0L;
        String curentLastId=null;
        boolean hasmore=false;
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        //获取zset的总长度，方便计算索引
        Long totalSize = zSetOperations.size(key);
        //判断reids中是否有内容
        if(totalSize!=null&&totalSize>0) {
            //判断是否是第一次取
            if (pageDTO.getLastId() == null || pageDTO.getLastId().isEmpty()) {
                start = 0L;
                end = Math.min(pageSize - 1, totalSize - 1);
            } else {
                Long lastId = Long.parseLong(pageDTO.getLastId());
                //定位到上一页最后一个点赞的索引
                LikeMusicVO likeMusicVO = music2Mapper.selectLikeMusicVO(lastId, currentUserId);
                String lastMusic = null;
                try {
                    lastMusic = objectMapper.writeValueAsString(likeMusicVO);
                    Long rank = zSetOperations.rank(key, lastMusic);
                    if(rank==null)
                    {
                        return Result.ok("未找到相应的记录");
                    }
                    else{
                        Long Rank = totalSize - rank - 1;
                        if(Rank<0)
                        {
                            return Result.ok("该lastId后面没有数据");
                        }
                        start = Rank + 1;
                        end = Math.min(start + pageSize - 1, totalSize - 1);
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
            }
            //根据start end获取redis中的数据
            Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(key, start, end);
            for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
                String value = typedTuple.getValue();
                try {
                    LikeMusicVO likeMusicVO = objectMapper.readValue(value, LikeMusicVO.class);
                    musicVOS.add(likeMusicVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
            }
            //判断是否还有更多数据
                hasmore = end < totalSize - 1;
                //记录下一页lastId
                if (!musicVOS.isEmpty()) {
                    curentLastId = musicVOS.get(musicVOS.size() - 1).getId();
                }
        }
            //如果reids没有命中,从数据库中查询
            if(musicVOS==null||musicVOS.isEmpty())
            {
                List<LikeMusicVO> likeMusicVOList=new ArrayList<>();
                Long lastId=null;
                if(!(pageDTO.getLastId()==null||pageDTO.getLastId().isEmpty()))
                {
                    lastId=Long.parseLong(pageDTO.getLastId());
                }
                likeMusicVOList = music2Mapper.selectLikeMusicVOAll(currentUserId);
                musicVOS=music2Mapper.selectLikeMusicVOBatchIds(lastId,pageSize,currentUserId);
                if(likeMusicVOList==null||likeMusicVOList.isEmpty())
                {
                    return Result.ok("暂无喜欢的歌曲");
                }
                if(musicVOS==null||musicVOS.isEmpty())
                {
                    return Result.ok("该lastId后面没有数据了");
                }
                //存入redis
                Set<ZSetOperations.TypedTuple<String>> tuples = likeMusicVOList.stream().map(
                                likeMusicVO -> {
                                   double epochSecond =likeMusicVO.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                                    String value = null;
                                    try {
                                        value = objectMapper.writeValueAsString(likeMusicVO);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return ZSetOperations.TypedTuple.of(value, epochSecond);
                                })
                        .collect(Collectors.toSet());
                if(!tuples.isEmpty())
                {
                    zSetOperations.add(key,tuples);
                    stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
                }

                //计算本页最后一个id
                curentLastId=musicVOS.get(musicVOS.size()-1).getId();
                //判断是否还有更多
                hasmore=musicVOS.size()<likeMusicVOList.size();
            }
            //封装数据
            PageLikeVO<LikeMusicVO> pageLikeVO=new PageLikeVO();
            pageLikeVO.setDateList(musicVOS);
            pageLikeVO.setHasMore(hasmore);
            pageLikeVO.setLastId(curentLastId);
            return Result.ok(pageLikeVO);
        }




    //获取用户收藏的专辑
    @Override
    public Result getCollectAlbum(PageDTO pageDTO)
    {
        //获取当前用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Integer pageSize=pageDTO.getSize();
        //查询reids中的数据
        String key=USER_COLLECTION+":"+"album"+currentUserId;
        List<CollectioAlbumVO> albumVOS=new ArrayList<>();
        Long start=0L;
        Long end=0L;
        String curentLastId=null;
        boolean hasmore=false;
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        //获取zset的总长度，方便计算索引
        Long totalSize = zSetOperations.size(key);
        //判断reids中是否有内容
        if(totalSize!=null&&totalSize>0) {
            //判断是否是第一次取
            if (pageDTO.getLastId() == null||pageDTO.getLastId().isEmpty()) {
                start = 0L;
                end = Math.min(pageSize - 1, totalSize - 1);
            } else {
                Long lastId=Long.parseLong(pageDTO.getLastId());
                //定位到上一页最后一个音乐的索引
                CollectioAlbumVO collectioAlbumVO = album2Mapper.selectAlbumList(lastId,currentUserId);
                String lastAlbum = null;
                try {
                    lastAlbum = objectMapper.writeValueAsString(collectioAlbumVO);
                    Long rank = zSetOperations.rank(key, lastAlbum);
                    if(rank==null)
                    {
                        return Result.ok("未找到相应的数据");
                    }
                    else
                    {
                        Long Rank = totalSize - rank - 1;
                         if(Rank<0)
                        {
                            return Result.ok("该lastId后面没有数据了");
                        }
                        start = Rank + 1;
                        end = Math.min(start + pageSize - 1, totalSize - 1);
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
            }
            //根据start end获取redis中的数据
            Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(key, start, end);
            for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
                String value = typedTuple.getValue();
                try {
                    CollectioAlbumVO collectioAlbumVO = objectMapper.readValue(value, CollectioAlbumVO.class);
                    albumVOS.add(collectioAlbumVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
            }
            //判断是否还有更多数据
            hasmore = end < totalSize - 1;
            //记录下一页lastId
            if (!albumVOS.isEmpty()) {
                curentLastId = albumVOS.get(albumVOS.size() - 1).getId();
            }
        }
        //如果reids没有命中,从数据库中查询
        if(albumVOS==null||albumVOS.isEmpty())
        {
            Long lastId=null;
            if(!(pageDTO.getLastId()==null||pageDTO.getLastId().isEmpty()))
            {
                lastId=Long.parseLong(pageDTO.getLastId());
            }
            List<CollectioAlbumVO> albumVOList=new ArrayList<>();
            albumVOS = album2Mapper.selectAlbumListBatchIds(currentUserId, pageSize,lastId);
            albumVOList= album2Mapper.selectAlbumListAll(currentUserId);
            if(albumVOList==null||albumVOList.isEmpty())
            {
                return Result.ok("未找到收藏记录");
            }
            if(albumVOS==null||albumVOS.isEmpty())
            {
                return Result.ok("该lastId后面没有数据了");
            }
            //存入redis
            Set<ZSetOperations.TypedTuple<String>> tuples = albumVOList.stream().map(
                            collectioAlbumVO -> {
                                double epochSecond =collectioAlbumVO.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                                String value = null;
                                try {
                                    value = objectMapper.writeValueAsString(collectioAlbumVO);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                                return ZSetOperations.TypedTuple.of(value, epochSecond);
                            })
                    .collect(Collectors.toSet());
            if(!tuples.isEmpty())
            {
                zSetOperations.add(key,tuples);
                stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
            }
            //计算本页最后一个id
            curentLastId=albumVOS.get(albumVOS.size()-1).getId();
            //判断是否还有更多
            hasmore=albumVOS.size()<albumVOList.size();
        }
        //封装数据
        PageLikeVO<CollectioAlbumVO> pageLikeVO=new PageLikeVO();
        pageLikeVO.setDateList(albumVOS);
        pageLikeVO.setHasMore(hasmore);
        pageLikeVO.setLastId(curentLastId);
        return Result.ok(pageLikeVO);
    }


//获取收藏的歌单
    @Override
    public Result getPlayList(PageDTO pageDTO) {
        //获取当前用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Integer pageSize=pageDTO.getSize();
        //查询reids中的数据
        String key=USER_COLLECTION+":"+"playList"+currentUserId;
        List<CollectonLIstVO> playLists=new ArrayList<>();
        Long start=0L;
        Long end=0L;
        String curentLastId=null;
        boolean hasmore=false;
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        //获取zset的总长度，方便计算索引
        Long totalSize = zSetOperations.size(key);
        //判断reids中是否有内容
        if(totalSize!=null&&totalSize>0) {
            //判断是否是第一次取
            if (pageDTO.getLastId() == null||pageDTO.getLastId().isEmpty()) {
                start = 0L;
                end = Math.min(pageSize - 1, totalSize - 1);
            } else {
                Long lastId=Long.parseLong(pageDTO.getLastId());
                //定位到上一页最后一个音乐的索引
                CollectonLIstVO collectonLIstVO = playList2Mapper.selectPlayListVO(lastId,currentUserId);
                String lastPlayList = null;
                try {
                    lastPlayList = objectMapper.writeValueAsString(collectonLIstVO);
                    Long rank = zSetOperations.rank(key, lastPlayList);
                    if(rank==null)
                    {
                        return Result.ok("未找到对应的记录");
                    }
                    Long Rank = totalSize - rank - 1;
                    if(Rank<0)
                    {
                        return Result.ok("该lastId后面没有数据了");
                    }
                    start = Rank + 1;
                    end = Math.min(start + pageSize - 1, totalSize - 1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
            }
            //根据start end获取redis中的数据
            Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(key, start, end);
            for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
                String value = typedTuple.getValue();
                try {
                    CollectonLIstVO collectonLIstVO = objectMapper.readValue(value, CollectonLIstVO.class);
                    playLists.add(collectonLIstVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
            }
            //判断是否还有更多数据
            hasmore = end < totalSize - 1;
            //记录下一页lastId
            if (!playLists.isEmpty()) {
                curentLastId = playLists.get(playLists.size() - 1).getId();
            }
        }
        //如果reids没有命中,从数据库中查询
        if(playLists==null||playLists.isEmpty())
        {
            Long lastId=null;
            if(!(pageDTO.getLastId()==null||pageDTO.getLastId().isEmpty()))
            {
                lastId=Long.parseLong(pageDTO.getLastId());
            }
            List<CollectonLIstVO> list=new ArrayList<>();
            playLists = playList2Mapper.selectPlayListBatchIds(currentUserId,lastId,pageSize);
            list=playList2Mapper.selectPlayListAll(currentUserId);
            if(list==null||list.isEmpty())
            {
                return Result.ok("暂无收藏的歌单");
            }
            if(playLists==null||playLists.isEmpty())
            {
                return Result.ok("该lastId后面没有数据了");
            }
            //存入redis
            Set<ZSetOperations.TypedTuple<String>> tuples = list.stream().map(
                            collectonLIstVO -> {
                                double epochSecond =collectonLIstVO.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                                String value = null;
                                try {
                                    value = objectMapper.writeValueAsString(collectonLIstVO);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                                return ZSetOperations.TypedTuple.of(value, epochSecond);
                            })
                    .collect(Collectors.toSet());
            if(!tuples.isEmpty())
            {
                zSetOperations.add(key,tuples);
                stringRedisTemplate.expire(key,7,TimeUnit.DAYS);
            }
            //计算本页最后一个id
            curentLastId=playLists.get(playLists.size()-1).getId();
            //判断是否还有更多
            hasmore=playLists.size()<list.size();
        }
        //封装数据
        PageLikeVO<CollectonLIstVO> pageLikeVO=new PageLikeVO();
        pageLikeVO.setDateList(playLists);
        pageLikeVO.setHasMore(hasmore);
        pageLikeVO.setLastId(curentLastId);
        return Result.ok(pageLikeVO);
    }



    //关注
    @Override
    public Result follow(Long userId) {
        if(userId==null)
        {
            return Result.fail("被关注者id不能为空");
        }
        User user = user2Mapper.selectById(userId);
        if(user==null)
        {
            return Result.fail("该用户不存在");
        }
        if(user.getUserStatus()==0)
        {
            return Result.fail("该用户已被封禁，禁止关注");
        }
        //获取我当前用户id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper1=new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(UserFollow::getUserId,currentUserId)
                .eq(UserFollow::getFollowedUserId,userId);
        UserFollow follow1 = followMapper.selectOne(lambdaQueryWrapper1);
        if(follow1!=null)
        {
            return Result.fail("您已经关注过了，请勿重复操作");
        }
        //关注
        UserFollow follow=new UserFollow();
        follow.setFollowedUserId(userId);
        follow.setUserId(currentUserId);
        follow.setFollowTime(LocalDateTime.now());
        followMapper.insert(follow);
        FollowerVO followerVO=new FollowerVO();
        followerVO.setIsfollow(true);
        followerVO.setMsg("关注成功");
        //更新关注数
        followMapper.updateFollowCountUp(currentUserId);
        //更新关注数
        followMapper.updateFansCountUp(userId);
        LambdaQueryWrapper<UserRole> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserRole::getUserId,userId);
        List<UserRole> userRole = userRoleMapper.selectList(lambdaQueryWrapper);
        List<Long> userIds=userRole.stream().map(UserRole::getRoleId).toList();
        //存入redis
        try {
            //关注列表缓存
            String followKey=null;
            if(!userIds.contains(3L)) {
                followKey= RedisCacheConstants.USER_FOLLOW + ":" + currentUserId+"normal";
            }else if(userIds.contains(3L))
            {
                followKey=RedisCacheConstants.USER_FOLLOW+":"+currentUserId+"music";
            }
            UserFollowerVO userFollowerVO = followMapper.selectFollowUser(userId,currentUserId);
            String follow2 = objectMapper.writeValueAsString(userFollowerVO);
            double score=follow.getFollowTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            stringRedisTemplate.opsForZSet().add(followKey,follow2,score);
            stringRedisTemplate.expire(followKey,7,TimeUnit.DAYS);
            //粉丝列表缓存
            String followerKey= RedisCacheConstants.USER_FOLLOWER+":"+userId;
            UserFollowerVO userFollowerVO1 = followMapper.selectFollowedUser(currentUserId,userId);
            String follow3 = objectMapper.writeValueAsString(userFollowerVO1);
            double score1=follow.getFollowTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            stringRedisTemplate.opsForZSet().add(followerKey,follow3,score1);
            stringRedisTemplate.expire(followKey,7,TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Result.ok(followerVO);
    }




    /**
     * 取消关注
     */
    @Override
    public Result unFollow(Long userId) {
        if(userId==null)
        {
            return Result.fail("被关注者id不能为空");
        }
        User user = user2Mapper.selectById(userId);
        if(user==null)
        {
            return Result.fail("该用户不存在");
        }
        Long currentUserId = SecurityUtil.getCurrentUserId();
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper1=new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(UserFollow::getUserId,currentUserId)
                .eq(UserFollow::getFollowedUserId,userId);
        UserFollow follow = followMapper.selectOne(lambdaQueryWrapper1);
        if(follow==null)
        {
            return Result.fail("你还没有关注当前用户");
        }
        LambdaQueryWrapper<UserRole> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserRole::getUserId,userId);
        List<UserRole> userRole = userRoleMapper.selectList(lambdaQueryWrapper);
        List<Long> userIds=userRole.stream().map(UserRole::getRoleId).toList();
        //从reids中删除
        try {
            //关注列表缓存
            String followKey=null;
            if(!userIds.contains(3L)) {
                 followKey= RedisCacheConstants.USER_FOLLOW + ":" + currentUserId+"normal";
            }else if(userIds.contains(3L))
            {
                followKey=RedisCacheConstants.USER_FOLLOW+":"+currentUserId+"music";
            }
            UserFollowerVO userFollowerVO = followMapper.selectFollowUser(userId,currentUserId);
            String follow2 = objectMapper.writeValueAsString(userFollowerVO);
            log.info(follow2);
            stringRedisTemplate.opsForZSet().remove(followKey,follow2);
            //粉丝列表缓存
            String followerKey= RedisCacheConstants.USER_FOLLOWER+":"+userId;
            UserFollowerVO userFollowerVO1 = followMapper.selectFollowedUser(currentUserId,userId);
            String follow3 = objectMapper.writeValueAsString(userFollowerVO1);
            log.info(follow3);
            stringRedisTemplate.opsForZSet().remove(followerKey,follow3);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        //取消关注
        followMapper.delete(lambdaQueryWrapper1);
        FollowerVO followerVO=new FollowerVO();
        followerVO.setIsfollow(false);
        followerVO.setMsg("取消关注成功");
        //更新关注数
        followMapper.updateFollowCountDown(currentUserId);
        //更新关注数
        followMapper.updateFansCountDown(userId);
        return Result.ok(followerVO);
    }

    /**
     * 用户的关注数
     */
    @Override
    public Result getFollowCount()
    {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long count = followMapper.followCount(currentUserId);
        return Result.ok(count);
    }

    /**
     * 用户的粉丝数
     */
    @Override
    public Result getFollowerCount()
    {
        Long currentUserId=SecurityUtil.getCurrentUserId();
        Long count = followMapper.fansCount(currentUserId);
        return Result.ok(count);
    }

    /**
     * 用户的粉丝列表
     */
    @Override
    public Result getFollowerList(PageDTO pageDTO)
    {
        //从redis中查询
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String followerKey= RedisCacheConstants.USER_FOLLOWER+":"+currentUserId;
        List<UserFollowerVO> userFollowerVOS=new ArrayList<>();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        //总数量用于计数
        Long totalSize=zSetOperations.size(followerKey);
        Integer pageSize=pageDTO.getSize();
        Long start=0L;
        Long end=0L;
        Long currentLastId=0L;
        Boolean hasmore=false;
        //判断是不是从头开始查找的
        if(totalSize!=null&&totalSize>0) {
            if (pageDTO.getLastId()==null||pageDTO.getLastId().trim().isEmpty()) {
                end = Math.min(pageSize - 1, totalSize - 1);
            } else {
                Long lastId = Long.parseLong(pageDTO.getLastId());
                //定位到当前lastId
                UserFollowerVO user = followMapper.selectFollowedUser(lastId,currentUserId);
                try {
                    String s = objectMapper.writeValueAsString(user);
                    Long rank = zSetOperations.rank(followerKey, s);
                    if (rank == null) {
                        return Result.fail("未找到相关记录");
                    }
                        Long Rank = totalSize - rank - 1;
                        if(Rank<0)
                        {
                            return Result.ok("该lastId后面没有数据了");
                        }
                        start =Rank+1;
                        end = Math.min(start + pageSize - 1, totalSize - 1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //从redis中获取
            Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(followerKey, start, end);
            for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
                String value = typedTuple.getValue();
                try {
                    UserFollowerVO userFollowerVO = objectMapper.readValue(value, UserFollowerVO.class);
                    userFollowerVOS.add(userFollowerVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //判断是否还有数据
            hasmore = end < totalSize - 1;
            //当前页的最后元素的id
            currentLastId = userFollowerVOS.get(userFollowerVOS.size() - 1).getId();
        }
            //如果没有找到，从数据库中找
            if(userFollowerVOS==null||userFollowerVOS.isEmpty())
            {
                Long lastId=null;
                if( !(pageDTO.getLastId()== null||pageDTO.getLastId().trim().isEmpty()))
                {
                    lastId=Long.parseLong(pageDTO.getLastId());
                }
                List<UserFollowerVO> userFollowerVOList=new ArrayList<>();
                List<Long> userIds = followMapper.selectFollowerUserIds(currentUserId);
                if (userIds == null || userIds.isEmpty()) {
                    return Result.ok("暂无粉丝");
                }
                userFollowerVOList=followMapper.selectFollowAll(userIds,currentUserId);
                if(userFollowerVOList==null||userFollowerVOList.isEmpty())
                {
                    return Result.ok("暂无粉丝");
                }
                userFollowerVOS=followMapper.selectFollowUserBatch(userIds,lastId,pageSize,currentUserId);
                if(userFollowerVOS==null||userFollowerVOS.isEmpty())
                {
                    return Result.ok("当前lastId后面没有数据");
                }
                if(userFollowerVOS==null||userFollowerVOS.isEmpty())
                {
                    return Result.ok("暂无粉丝");
                }
                //存入redis
                Set<ZSetOperations.TypedTuple<String>> tuples = userFollowerVOList.stream().map(
                                userFollowerVO -> {
                                    String value = null;
                                    double score = userFollowerVO.getFollowTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                                    try {
                                        value = objectMapper.writeValueAsString(userFollowerVO);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return ZSetOperations.TypedTuple.of(value, score);
                                })
                        .collect(Collectors.toSet());
                if(!(tuples==null||tuples.isEmpty())) {
                    zSetOperations.add(followerKey, tuples);
                    stringRedisTemplate.expire(followerKey,7, TimeUnit.DAYS);
                }
                //判断是否有其他数据
                currentLastId=userFollowerVOS.get(userFollowerVOS.size()-1).getId();
                hasmore= userFollowerVOS.size()<userFollowerVOList.size();
            }
            //封装数据
            PageLikeVO<UserFollowerVO> pageLikeVO=new PageLikeVO<>();
            pageLikeVO.setDateList(userFollowerVOS);
            pageLikeVO.setHasMore(hasmore);
            pageLikeVO.setLastId(currentLastId.toString());
            return Result.ok(pageLikeVO);
        }


    /**
     * 获取关注的音乐人列表
     * @param pageDTO
     * @return
     */
    @Override
    public Result getFollowListMusic(PageDTO pageDTO)
    {
        //从redis中查询
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String followKey=RedisCacheConstants.USER_FOLLOW+":"+currentUserId+"music";
        List<UserFollowerVO> userFollowerVOS=new ArrayList<>();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        //总数量用于计数
        Long totalSize=zSetOperations.size(followKey);
        Integer pageSize=pageDTO.getSize();
        Long start=0L;
        Long end=0L;
        Long currentLastId=0L;
        Boolean hasmore=false;
        //判断是不是从头开始查找的
        if(totalSize!=null&&totalSize>0) {
            if (pageDTO.getLastId()==null||pageDTO.getLastId().trim().isEmpty()) {
                end = Math.min(pageSize - 1, totalSize - 1);
            } else {
                Long lastId = Long.parseLong(pageDTO.getLastId());
                //定位到当前lastId,lastid是userId
                UserFollowerVO user = followMapper.selectFollowUser(lastId,currentUserId);
                try {
                    String s = objectMapper.writeValueAsString(user);
                    Long rank = zSetOperations.rank(followKey, s);
                    if (rank == null) {
                        return Result.ok("未找到相关记录");
                    }
                    Long Rank = totalSize - rank-1;
                    if(Rank<0)
                    {
                        return Result.ok("该lastId后面没有数据了");
                    }
                    start = Rank+1;
                    end = Math.min(start + pageSize - 1, totalSize - 1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //从redis中获取
            Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(followKey, start, end);
            for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
                String value = typedTuple.getValue();
                try {
                    UserFollowerVO userFollowerVO = objectMapper.readValue(value, UserFollowerVO.class);
                    userFollowerVOS.add(userFollowerVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //判断是否还有数据
            hasmore = end < totalSize - 1;
            //当前页的最后元素的id
            currentLastId = userFollowerVOS.get(userFollowerVOS.size() - 1).getId();
        }
        //如果没有找到，从数据库中找
        if(userFollowerVOS==null||userFollowerVOS.isEmpty())
        {
            Long lastId=null;
            if( !(pageDTO.getLastId()== null||pageDTO.getLastId().trim().isEmpty()))
            {
                lastId=Long.parseLong(pageDTO.getLastId());
            }
            List<UserFollowerVO> userFollowerVOList=new ArrayList<>();
            List<Long> userIds = followMapper.selectFollowUserIdsMusician(currentUserId);
            if (userIds == null || userIds.isEmpty()) {
                return Result.ok("暂无关注的音乐人");
            }
            userFollowerVOList=followMapper.selectFollowedAllMuscian(userIds,currentUserId);
            if(userFollowerVOList==null||userFollowerVOList.isEmpty())
            {
                return Result.ok("暂无关注的音乐人");
            }
            userFollowerVOS=followMapper.selectFollowedUserBatchMuscian(userIds,lastId,pageSize,currentUserId);
            if(userFollowerVOS==null||userFollowerVOS.isEmpty())
            {
                return Result.ok("当前lastId后面没有数据");
            }
            if(userFollowerVOS==null||userFollowerVOS.isEmpty())
            {
                return Result.ok("暂无关注的音乐人");
            }
            //存入redis
            Set<ZSetOperations.TypedTuple<String>> tuples = userFollowerVOList.stream().map(
                            userFollowerVO -> {
                                String value = null;
                                double score = userFollowerVO.getFollowTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                                try {
                                    value = objectMapper.writeValueAsString(userFollowerVO);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                                return ZSetOperations.TypedTuple.of(value, score);
                            })
                    .collect(Collectors.toSet());
            if(!(tuples==null||tuples.isEmpty())) {
                zSetOperations.add(followKey, tuples);
                stringRedisTemplate.expire(followKey,7, TimeUnit.DAYS);
            }
            //判断是否有其他数据
            currentLastId=userFollowerVOS.get(userFollowerVOS.size()-1).getId();
            hasmore= userFollowerVOS.size()<userFollowerVOList.size();
        }
        //封装数据
        PageLikeVO<UserFollowerVO> pageLikeVO=new PageLikeVO<>();
        pageLikeVO.setDateList(userFollowerVOS);
        pageLikeVO.setHasMore(hasmore);
        pageLikeVO.setLastId(currentLastId.toString());
        return Result.ok(pageLikeVO);
    }
    /**
     * 获取关注的普通人列表
     * @param pageDTO
     * @return
     */
    @Override
    public Result getFollowListNormal(PageDTO pageDTO)
    {
        //从redis中查询
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String followKey=RedisCacheConstants.USER_FOLLOW + ":" + currentUserId+"normal";
        List<UserFollowerVO> userFollowerVOS=new ArrayList<>();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        //总数量用于计数
        Long totalSize=zSetOperations.size(followKey);
        Integer pageSize=pageDTO.getSize();
        Long start=0L;
        Long end=0L;
        Long currentLastId=0L;
        Boolean hasmore=false;
        //判断是不是从头开始查找的
        if(totalSize!=null&&totalSize>0) {
            if (pageDTO.getLastId()==null||pageDTO.getLastId().trim().isEmpty()) {
                end = Math.min(pageSize - 1, totalSize - 1);
            } else {
                Long lastId = Long.parseLong(pageDTO.getLastId());
                //定位到当前lastId
                UserFollowerVO user = followMapper.selectFollowUser(lastId,currentUserId);
                try {
                    String s = objectMapper.writeValueAsString(user);
                    Long rank = zSetOperations.rank(followKey, s);
                    if (rank == null) {
                        return Result.fail("未找到相关记录");
                    }
                    Long Rank = totalSize - rank-1;
                    if(Rank<0)
                    {
                        return Result.ok("该lastId后面没有数据了");
                    }
                    start = Rank+1;
                    end = Math.min(start + pageSize - 1, totalSize - 1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //从redis中获取
            Set<ZSetOperations.TypedTuple<String>> typedTuples = zSetOperations.reverseRangeWithScores(followKey, start, end);
            for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
                String value = typedTuple.getValue();
                try {
                    UserFollowerVO userFollowerVO = objectMapper.readValue(value, UserFollowerVO.class);
                    userFollowerVOS.add(userFollowerVO);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            //判断是否还有数据
            hasmore = end < totalSize - 1;
            //当前页的最后元素的id
            currentLastId = userFollowerVOS.get(userFollowerVOS.size() - 1).getId();
        }
        //如果没有找到，从数据库中找
        if(userFollowerVOS==null||userFollowerVOS.isEmpty())
        {
            Long lastId=null;
            if( !(pageDTO.getLastId()== null||pageDTO.getLastId().trim().isEmpty()))
            {
                lastId=Long.parseLong(pageDTO.getLastId());
            }
            List<UserFollowerVO> userFollowerVOList=new ArrayList<>();
            List<Long> userIds = followMapper.selectFollowUserIdsnormal(currentUserId);
            if (userIds == null || userIds.isEmpty()) {
                return Result.ok("暂无关注的普通用户");
            }
            userFollowerVOList=followMapper.selectFollowedAllNormal(userIds,currentUserId);
            if(userFollowerVOList==null||userFollowerVOList.isEmpty())
            {
                return Result.ok("暂无关注的普通用户");
            }
            userFollowerVOS=followMapper.selectFollowedUserBatchNormal(userIds,lastId,pageSize,currentUserId);
            if(userFollowerVOS==null||userFollowerVOS.isEmpty())
            {
                return Result.ok("当前lastId后面没有数据");
            }
            if(userFollowerVOS==null||userFollowerVOS.isEmpty())
            {
                return Result.ok("暂无关注的普通用户");
            }
            //存入redis
            Set<ZSetOperations.TypedTuple<String>> tuples = userFollowerVOList.stream().map(
                            userFollowerVO -> {
                                String value = null;
                                double score = userFollowerVO.getFollowTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
                                try {
                                    value = objectMapper.writeValueAsString(userFollowerVO);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                                return ZSetOperations.TypedTuple.of(value, score);
                            })
                    .collect(Collectors.toSet());
            if(!(tuples==null||tuples.isEmpty())) {
                zSetOperations.add(followKey, tuples);
                stringRedisTemplate.expire(followKey,7, TimeUnit.DAYS);
            }
            //判断是否有其他数据
            currentLastId=userFollowerVOS.get(userFollowerVOS.size()-1).getId();
            hasmore= userFollowerVOS.size()<userFollowerVOList.size();
        }
        //封装数据
        PageLikeVO<UserFollowerVO> pageLikeVO=new PageLikeVO<>();
        pageLikeVO.setDateList(userFollowerVOS);
        pageLikeVO.setHasMore(hasmore);
        pageLikeVO.setLastId(currentLastId.toString());
        return Result.ok(pageLikeVO);
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
        Long count = followMapper.followCount(currentUserId);
        otherUserVO.setFollowCount(count);
        //设置粉丝数
        Long followerCount = followMapper.fansCount(currentUserId);
        otherUserVO.setFollowerCount(followerCount);
        //是否关注作者
        LambdaQueryWrapper<UserFollow> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
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


        Integer i = user2Mapper.selectRoleIds(currentUserId);
        RoleEnum roleEnum = RoleEnum.valueOf(i);
        userInfoVO.setUserRole(roleEnum.getDoc());
        return Result.ok(userInfoVO);

    }

}

