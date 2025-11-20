package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.social.Comment;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.Playlist;
import entity.pojo.social.UserCollection;
import entity.pojo.util.Result;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import social.domain.CollectionEnum;
import social.domain.LikeEnum;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.dto.PageDTO;
import social.domain.vo.*;
import social.mapper.*;
import social.service.UserBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static constants.RedisCacheConstants.USER_COLLECTION;
import static constants.RedisCacheConstants.USER_LIKE;

@Slf4j
@Service
public class UserBaseServiceImpl implements UserBaseService {
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
        //判断目标id的数据是否存在 目标类型 1-音乐 2-评论
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
                        stringRedisTemplate.opsForZSet().add(key,objectMapper.writeValueAsString(likeMusicVO),epochSecond);
                        stringRedisTemplate.expire(key,7, TimeUnit.DAYS);
                    }
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
            count= commentMapper.selectCount(targetId);
            if (isCancle) {
                if(count>0) {
                    commentMapper.updateLikeCountLow(targetId);
                    count--;
                }
            } else {
                commentMapper.updateLikeCountup(targetId);
                count++;
            }
            return count;
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
                        CollectonLIstVO collectonLIstVO=playList2Mapper.selectLikeMusicVO(targetId,currentUserId);
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
                        CollectonLIstVO collectonLIstVO=playList2Mapper.selectLikeMusicVO(targetId,currentUserId);
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
                CollectonLIstVO collectonLIstVO=playList2Mapper.selectLikeMusicVO(targetId,currentUserId);
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
        Long curentLastId=null;
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
                //定位到上一页最后一个点赞的索引
                LikeMusicVO likeMusicVO = music2Mapper.selectLikeMusicVO(lastId,currentUserId);
                String lastMusic = null;
                try {
                    lastMusic = objectMapper.writeValueAsString(likeMusicVO);
                    Long rank = zSetOperations.rank(key, lastMusic);
                    Long Rank = totalSize - rank - 1;
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
                Long lastId=null;
                if(!(pageDTO.getLastId()==null||pageDTO.getLastId().isEmpty()))
                {
                    lastId=Long.parseLong(pageDTO.getLastId());
                }
                musicVOS = music2Mapper.selectLikeMusicVOBatchIds(lastId, pageSize,currentUserId);
                if(musicVOS==null||musicVOS.isEmpty())
                {
                    return Result.ok("暂无喜欢的歌曲");
                }
                //存入redis
                Set<ZSetOperations.TypedTuple<String>> tuples = musicVOS.stream().map(
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
                hasmore=music2Mapper.countHasMore(currentUserId,curentLastId)>0;
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
        Long curentLastId=null;
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
                    Long Rank = totalSize - rank - 1;
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
            albumVOS = album2Mapper.selectAlbumListBatchIds(currentUserId, pageSize,lastId);
            if(albumVOS==null||albumVOS.isEmpty())
            {
                return Result.ok("暂无收藏的专辑");
            }
            //存入redis
            Set<ZSetOperations.TypedTuple<String>> tuples = albumVOS.stream().map(
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
            hasmore=music2Mapper.countHasMore(currentUserId,curentLastId)>0;
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
        Long curentLastId=null;
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
                CollectonLIstVO collectonLIstVO = playList2Mapper.selectLikeMusicVO(lastId,currentUserId);
                String lastPlayList = null;
                try {
                    lastPlayList = objectMapper.writeValueAsString(collectonLIstVO);
                    Long rank = zSetOperations.rank(key, lastPlayList);
                    Long Rank = totalSize - rank - 1;
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
            playLists = playList2Mapper.selectLikeMusicVOBatchIds(currentUserId, pageSize,lastId);
            if(playLists==null||playLists.isEmpty())
            {
                return Result.ok("暂无收藏的歌单");
            }
            //存入redis
            Set<ZSetOperations.TypedTuple<String>> tuples = playLists.stream().map(
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
            hasmore=music2Mapper.countHasMore(currentUserId,curentLastId)>0;
        }
        //封装数据
        PageLikeVO<CollectonLIstVO> pageLikeVO=new PageLikeVO();
        pageLikeVO.setDateList(playLists);
        pageLikeVO.setHasMore(hasmore);
        pageLikeVO.setLastId(curentLastId);
        return Result.ok(pageLikeVO);
    }

}
