package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.music.Music;
import entity.pojo.social.Comment;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.Playlist;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.LikeEnum;
import social.domain.dto.LikeDTO;
import social.mapper.CommentMapper;
import social.mapper.LikeRecordMapper;
import social.mapper.MusicMapper;
import social.mapper.PlayListMapper;
import social.service.UserBaseService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UserBaseServiceImpl implements UserBaseService {
    @Autowired
    private LikeRecordMapper likeRecordMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private MusicMapper musicMapper;
    @Autowired
    private PlayListMapper playListMapper;
    //点赞/取消点赞功能
    @Transactional
    @Override
    public Result likeandUnlike(LikeDTO likeDTO) {
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
            Music music = musicMapper.selectOne(musicWrapper);
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
            Playlist playlist = playListMapper.selectOne(playListWrapper);
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
        Map<String, Long> likevo = new HashMap<>();
        //如果有点赞记录，判断是否点赞过，如果之前取消了则恢复
        if (likeRecord != null) {
            //如果点赞过了，则取消
            if (likeRecord.getIsCanceled() == 0) {
                //取消
                likeRecord.setIsCanceled(1);
                likeRecord.setUpdatedTime(LocalDateTime.now());
                Long count = updateCount(true, targetId, targetType);
                likevo.put("isLike",1L);
                likevo.put("likecount",count);
                return Result.ok(likevo);
            }
            //如果此前取消了,现在要恢复
            if(likeRecord.getIsCanceled()==1)
            {
                //恢复
                likeRecord.setIsCanceled(0);
                likeRecord.setUpdatedTime(LocalDateTime.now());
                Long count = updateCount(false, targetId, targetType);
                likevo.put("isLike",0L);
                likevo.put("likecount",count);
                return Result.ok(likevo);
            }
        }
        //如果没有点赞过，创建新的点赞记录
        LikeRecord likeRecord1=new LikeRecord();
        likeRecord1.setUserId(currentUserId);
        likeRecord1.setTargetId(targetId);
        likeRecord1.setTargetType(targetType);
        likeRecordMapper.insert(likeRecord1);
        Long count = updateCount(false, targetId, targetType);
        likevo.put("isLike",0L);
        likevo.put("likecount",count);
        return Result.ok(likevo);
    }
    //变更点赞数
    private Long updateCount(Boolean isCancle,Long targetId,Integer targetType)
    {

        //变更对应目标的点赞量
        long count=0;
        if(targetType==LikeEnum.MUSIC.getValue())
        {
            LambdaQueryWrapper<Music> musicWrapper=new LambdaQueryWrapper();
            musicWrapper.eq(Music::getId,targetId);
            Music music = musicMapper.selectOne(musicWrapper);
            Long likeCount = music.getLikeCount();
            if(isCancle) {

                count = likeCount - 1;
            }else {
                count = likeCount + 1;
            }
            music.setLikeCount(count);
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
        }else if(targetType==LikeEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> playListWrapper = new LambdaQueryWrapper();
            playListWrapper.eq(Playlist::getId, targetId);
            Playlist playlist = playListMapper.selectOne(playListWrapper);
            Long likeCount = playlist.getLikeCount();
            if(isCancle) {

                count = likeCount - 1;
            }else {
                count = likeCount + 1;
            }
            playlist.setLikeCount(count);
        }
       return count;
    }
}
