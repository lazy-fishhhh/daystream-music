package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.music.Music;
import entity.pojo.social.Comment;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.Playlist;
import entity.pojo.social.UserCollection;
import entity.pojo.util.Result;
import social.domain.CollectionEnum;
import social.domain.LikeEnum;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.vo.CollectionVO;
import social.domain.vo.LikeVO;
import social.mapper.*;
import social.service.UserBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

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
        //判断目标id的数据是否存在 目标类型 1-音乐 2-评论 3-歌单
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
        } else if (targetType == LikeEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> playListWrapper = new LambdaQueryWrapper();
            playListWrapper.eq(Playlist::getId, targetId);
            Playlist playlist = playList2Mapper.selectOne(playListWrapper);
            if (playlist == null) {
                return Result.fail("歌单不存在");
            }
            //如果歌单私有
            else if (playlist.getIsPublic() == 0) {
                if (playlist.getUserId() != currentUserId) {
                    return Result.fail("该歌单未公开，无权点赞");
                }
            }
        }
        //根据点赞类型不同，处理不同的点赞
        //如果点赞记录已存在只是之前逻辑取消点赞了,那么重新恢复点赞记录
        LambdaQueryWrapper<LikeRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(LikeRecord::getUserId, currentUserId)
                .eq(LikeRecord::getTargetId, targetId)
                .eq(LikeRecord::getTargetType, targetType);
        LikeRecord likeRecord = likeRecordMapper.selectOne(lambdaQueryWrapper);
        LikeVO likevo=new LikeVO();
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
                return Result.ok(likevo);
            }
            //如果此前取消了,现在要恢复
            if(likeRecord.getIsCanceled()==1)
            {
                //恢复
                likeRecord.setIsCanceled(0);
                likeRecord.setUpdatedTime(LocalDateTime.now());
                likeRecordMapper.updateById(likeRecord);
                Long count = updateLikeCount(false, targetId, targetType);
                likevo.setIslike(true);
                likevo.setMsg("点赞成功");
                likevo.setLikecount(count);
                return Result.ok(likevo);
            }
        }
        //如果没有点赞过，创建新的点赞记录
        LikeRecord likeRecord1=new LikeRecord();
        likeRecord1.setUserId(currentUserId);
        likeRecord1.setTargetId(targetId);
        likeRecord1.setTargetType(targetType);
        likeRecordMapper.insert(likeRecord1);
        Long count = updateLikeCount(false, targetId, targetType);
        likevo.setIslike(true);
        likevo.setLikecount(count);
        likevo.setMsg("点赞成功");
        return Result.ok(likevo);
    }



    //变更点赞数
    private Long updateLikeCount(Boolean isCancle,Long targetId,Integer targetType)
    {

        //变更对应目标的点赞量
        long count=0;
        if(targetType==LikeEnum.MUSIC.getValue())
        {
            LambdaQueryWrapper<Music> musicWrapper=new LambdaQueryWrapper();
            musicWrapper.eq(Music::getId,targetId);
            Music music = music2Mapper.selectOne(musicWrapper);
            Long likeCount = music.getLikeCount();
            if(isCancle) {
                count = likeCount - 1;
            }else {
                count = likeCount + 1;
            }
            music.setLikeCount(count);
            music2Mapper.updateById(music);
        }else if(targetType==LikeEnum.COMMENT.getValue())
        {
            LambdaQueryWrapper<Comment> commentWrapper=new LambdaQueryWrapper();
            commentWrapper.eq(Comment::getId,targetId);
            Comment comment = commentMapper.selectOne(commentWrapper);
            Long likeCount =comment.getLikeCount();
            if(isCancle) {

                count = likeCount - 1;
            }else {
                count = likeCount + 1;
            }
            comment.setLikeCount(count);
            commentMapper.updateById(comment);
        }else if(targetType==LikeEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> playListWrapper = new LambdaQueryWrapper();
            playListWrapper.eq(Playlist::getId, targetId);
            Playlist playlist = playList2Mapper.selectOne(playListWrapper);
            Long likeCount = playlist.getLikeCount();
            if(isCancle) {

                count = likeCount - 1;
            }else {
                count = likeCount + 1;
            }
            playlist.setLikeCount(count);
            playList2Mapper.updateById(playlist);
        }
       return count;
    }

    /**
     * 收藏
     */
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
        //判断目标id的数据是否存在 目标类型 1-音乐 2-歌单
        if (targetType == CollectionEnum.MUSIC.getValue()) {
            LambdaQueryWrapper<Music> musicWrapper = new LambdaQueryWrapper();
            musicWrapper.eq(Music::getId, targetId)
                    .eq(Music::getStatus, 1);
            Music music = music2Mapper.selectOne(musicWrapper);
            if (music == null) {
                return Result.fail("音乐暂未上架");
            }
        } else if (targetType == CollectionEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> playListWrapper = new LambdaQueryWrapper();
            playListWrapper.eq(Playlist::getId, targetId);
            Playlist playlist = playList2Mapper.selectOne(playListWrapper);
            if (playlist == null) {
                return Result.fail("歌单不存在");
            }
            //如果歌单私有
            else if (playlist.getIsPublic() == 0) {
                if (playlist.getUserId() != currentUserId) {
                    return Result.fail("该歌单未公开，无权点赞");
                }
            }
        }
        //根据收藏类型不同，处理不同的收藏
        //如果收藏记录已存在只是之前逻辑取消收藏了,那么重新恢复收藏记录
        LambdaQueryWrapper<UserCollection> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserCollection::getUserId, currentUserId)
                .eq(UserCollection::getTargetId, targetId)
                .eq(UserCollection::getTargetType, targetType);
        UserCollection userCollection = userCollection2Mapper.selectOne(lambdaQueryWrapper);
        CollectionVO collectionVO=new CollectionVO();
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
                return Result.ok(collectionVO);
            }
            //如果此前取消了,现在要恢复
            if(userCollection.getIsCanceled()==1)
            {
                //恢复
                userCollection.setIsCanceled(0);
                userCollection.setUpdatedTime(LocalDateTime.now());
                userCollection2Mapper.updateById(userCollection);
                Long count = updateCollectCount(false, targetId, targetType);
                collectionVO.setIsCollection(true);
                collectionVO.setMsg("点赞成功");
                collectionVO.setCollectCount(count);
                return Result.ok(collectionVO);
            }
        }
        //如果没有收藏过，创建新的收藏记录
        UserCollection userCollection1=new UserCollection();
        userCollection1.setUserId(currentUserId);
        userCollection1.setTargetId(targetId);
        userCollection1.setTargetType(targetType);
        userCollection2Mapper.insert(userCollection1);
        Long count = updateCollectCount(false, targetId, targetType);
        collectionVO.setIsCollection(true);
        collectionVO.setCollectCount(count);
        collectionVO.setMsg("点赞成功");
        return Result.ok(collectionVO);
    }
    //变更收藏数
    private Long updateCollectCount(Boolean isCancle,Long targetId,Integer targetType)
    {

        //变更对应目标的收藏量
        long count=0;
        if(targetType==CollectionEnum.MUSIC.getValue())
        {
            LambdaQueryWrapper<Music> musicWrapper=new LambdaQueryWrapper();
            musicWrapper.eq(Music::getId,targetId);
            Music music = music2Mapper.selectOne(musicWrapper);
            Long collectCount = music.getCollectCount();
            if(isCancle) {
                count = collectCount - 1;
            }else {
                count = collectCount + 1;
            }
            music.setCollectCount(count);
            music2Mapper.updateById(music);
        }else if(targetType==CollectionEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> playListWrapper = new LambdaQueryWrapper();
            playListWrapper.eq(Playlist::getId, targetId);
            Playlist playlist = playList2Mapper.selectOne(playListWrapper);
            Long  collectCount= playlist.getCollectCount();
            if(isCancle) {

                count = collectCount - 1;
            }else {
                count = collectCount + 1;
            }
            playlist.setCollectCount(count);
            playList2Mapper.updateById(playlist);
        }
        return count;
    }
}
