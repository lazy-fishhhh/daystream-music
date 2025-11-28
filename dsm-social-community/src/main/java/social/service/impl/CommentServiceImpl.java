package social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.login.util.SecurityUtil;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.social.Comment;
import entity.pojo.social.CommunityPost;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.Playlist;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import social.domain.CommentEnum;
import social.domain.LikeEnum;
import social.domain.dto.CommentDTO;
import social.domain.dto.CommentListDTO;
import social.domain.dto.CommentReplyDTO;
import social.domain.dto.ReplyDTO;
import social.domain.vo.*;
import social.mapper.*;
import social.service.CommentService;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private Music2Mapper music2Mapper;
    @Autowired
    private PlayList2Mapper playList2Mapper;
    @Autowired
    private Album2Mapper album2Mapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private LikeRecord1Mapper likeRecord1Mapper;
    @Autowired
    private CommunityPostMapper communityPostMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    /**
     * 发布评论
     *
     * @param commentDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result releaseComment(CommentDTO commentDTO) {
        //获取当前用户
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long targetId = Long.parseLong(commentDTO.getTargetId());
        Integer targetType = commentDTO.getTargetType();
        String content = commentDTO.getContent();
        //判断目标是否存在
        if (targetType == CommentEnum.MUSIC.getValue()) {
            LambdaQueryWrapper<Music> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Music::getId, targetId)
                    .eq(Music::getStatus, 1);
            Music music = music2Mapper.selectOne(lambdaQueryWrapper);
            if (music == null) {
                return Result.fail("目标音乐未上架");
            }
        } else if (targetType == CommentEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Playlist::getId, targetId)
                    .eq(Playlist::getIsPublic, 1);
            Playlist playlist = playList2Mapper.selectOne(lambdaQueryWrapper);
            if (playlist == null) {
                return Result.fail("目标歌单未公开");
            }
        } else if (targetType == CommentEnum.ALBUM.getValue()) {
            LambdaQueryWrapper<Album> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Album::getId, targetId)
                    .eq(Album::getStatus, 1);
            Album album = album2Mapper.selectOne(lambdaQueryWrapper);
            if (album == null) {
                return Result.fail("目标专辑未上架");
            }
        }
        else if (targetType==CommentEnum.COMMUNITY.getValue()) {
            LambdaQueryWrapper<CommunityPost> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(CommunityPost::getId, targetId)
                    .eq(CommunityPost::getIsDeleted, 0);
            CommunityPost communityPost = communityPostMapper.selectOne(lambdaQueryWrapper);
            if (communityPost == null)
                return Result.fail("动态已删除");

        }

        //插入评论
        Comment comment = new Comment();
        comment.setTargetId(targetId);
        comment.setTargetType(targetType);
        comment.setUserId(currentUserId);
        comment.setContent(content);
        //更新评论数
        long count = 0;
        if (targetType == CommentEnum.MUSIC.getValue()) {
            count = music2Mapper.selectCommentCount(targetId);
            music2Mapper.commentCountUp(targetId);
            count++;
        } else if (targetType == CommentEnum.PLAYLIST.getValue()) {
            count = playList2Mapper.selectCommentCount(targetId);
            playList2Mapper.playlistCommentCountup(targetId);
            count++;
        } else if (targetType == CommentEnum.ALBUM.getValue()) {
            count = album2Mapper.selectCommetCount(targetId);
            album2Mapper.albumCommentCountup(targetId);
            count++;
        }
        else if (targetType == CommentEnum.COMMUNITY.getValue()) {
            count = communityPostMapper.selectCommetCount(targetId);
            communityPostMapper.communityCommentCountup(targetId);
            count++;
        }
        int insert = commentMapper.insert(comment);
        CommentVO commentVO = new CommentVO();
        if (insert > 0) {
            CommentListVO commentListVO = commentMapper.commentListvo(comment.getId());
            commentVO.setTargetCommentCount(count);
            commentVO.setMsg("评论成功");
            return Result.ok(commentVO);

        }
        return Result.fail("评论失败");
    }
    /**
     * 回复评论
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result replyComment(CommentReplyDTO commentReplyDTO) {
        //获取当前用户
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long targetId = Long.parseLong(commentReplyDTO.getParentId());
        String content = commentReplyDTO.getContent();
        LambdaQueryWrapper<Comment> commentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        commentLambdaQueryWrapper.eq(Comment::getId, targetId)
                .eq(Comment::getStatus, 1);
        Comment comment = commentMapper.selectOne(commentLambdaQueryWrapper);
        if (comment == null) {
            return Result.fail("回复的评论不存在");
        }
        Integer targetType = comment.getTargetType();
        //父评论存在时回复评论
        Comment replyComment = new Comment();
        replyComment.setTargetId(comment.getTargetId());
        replyComment.setTargetType(targetType);
        replyComment.setContent(content);
        replyComment.setUserId(currentUserId);
        replyComment.setReplyToUserId(comment.getUserId());
        replyComment.setParentId(targetId);
        //更新回复数
        int insert = commentMapper.insert(replyComment);
        Long parentTargetId=comment.getTargetId();
        if (insert > 0) {
            commentMapper.updateReplyCountup(targetId);
            Long count = commentMapper.selectReplyCount(targetId);
            //更新评论数
            long targetCount = 0;
            if (targetType == CommentEnum.MUSIC.getValue()) {
                music2Mapper.commentCountUp(parentTargetId);
                targetCount = music2Mapper.selectCommentCount(parentTargetId);
            } else if (targetType == CommentEnum.PLAYLIST.getValue()) {
                targetCount = playList2Mapper.selectCommentCount(parentTargetId);
                playList2Mapper.playlistCommentCountup(parentTargetId);
                targetCount++;
            } else if (targetType == CommentEnum.ALBUM.getValue()) {
                targetCount = album2Mapper.selectCommetCount(parentTargetId);
                album2Mapper.albumCommentCountup(parentTargetId);
                targetCount++;
            }
            else if (targetType == CommentEnum.COMMUNITY.getValue()) {
                targetCount = communityPostMapper.selectCommetCount(parentTargetId);
                communityPostMapper.communityCommentCountup(parentTargetId);
                targetCount++;
            }
            CommentListVO commentListVO = commentMapper.commentListvo(comment.getId());
            //返回视图
            ReplyCommentVO replyCommentVO=new ReplyCommentVO();
            replyCommentVO.setTargetCommentCount(targetCount);
            replyCommentVO.setParentCommentReplyCount(count);
            replyCommentVO.setMsg("回复成功");
            return Result.ok(replyCommentVO);
        }
        return Result.fail("回复失败");
    }

    /**
     * 获取用户发表的评论列表
     */
    @Override
    public Result getCommentList(CommentListDTO commentListDTO) {
        //获取当前用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long targetId = Long.parseLong(commentListDTO.getTargetId());
        Integer targetType = commentListDTO.getTargetType();
        Integer pageSize=commentListDTO.getSize();
        Long curentLastId=null;
        Boolean hasmore=false;
        //判断目标是否存在
        if (targetType == CommentEnum.MUSIC.getValue()) {
            LambdaQueryWrapper<Music> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Music::getId, targetId)
                    .eq(Music::getStatus, 1);
            Music music = music2Mapper.selectOne(lambdaQueryWrapper);
            if (music == null) {
                return Result.fail("目标音乐未上架");
            }
        } else if (targetType == CommentEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Playlist::getId, targetId)
                    .eq(Playlist::getIsPublic, 1);
            Playlist playlist = playList2Mapper.selectOne(lambdaQueryWrapper);
            if (playlist == null) {
                return Result.fail("目标歌单未公开");
            }
        } else if (targetType == CommentEnum.ALBUM.getValue()) {
            LambdaQueryWrapper<Album> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Album::getId, targetId)
                    .eq(Album::getStatus, 1);
            Album album = album2Mapper.selectOne(lambdaQueryWrapper);
            if (album == null) {
                return Result.fail("目标专辑未上架");
            }
        }
        else if(targetType == CommentEnum.COMMUNITY.getValue())
        {
            LambdaQueryWrapper<CommunityPost> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(CommunityPost::getId, targetId)
                    .eq(CommunityPost::getIsDeleted, 0);
            CommunityPost communityPost = communityPostMapper.selectOne(lambdaQueryWrapper);
            if (communityPost == null)
                return Result.fail("目标动态已删除");
        }
            Long lastId = null;
            if (commentListDTO.getLastId() != null && !commentListDTO.getLastId().isEmpty()) {
                lastId = Long.parseLong(commentListDTO.getLastId());
            }
            //查找lastId后面的评论
            List<CommentListVO> commentListVOS = commentMapper.commentList(targetId, lastId, pageSize);
            if (commentListVOS == null) {
                return Result.ok("后面没有数据了");
            }
            //判断是否还有更多数据
            Long has =commentMapper.hasmoreparent(targetId, lastId);
            hasmore=has>0;
            //记录下一页的lastId
            curentLastId = commentListVOS.get(commentListVOS.size() - 1).getId();
            for (CommentListVO commentListVO : commentListVOS) {
                if (commentListVO.getUserId() == currentUserId) {
                    commentListVO.setIsSelf(true);
                } else {
                    commentListVO.setIsSelf(false);
                }
                LambdaQueryWrapper<LikeRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(LikeRecord::getTargetId, commentListVO.getId())
                        .eq(LikeRecord::getTargetType, LikeEnum.COMMENT.getValue())
                        .eq(LikeRecord::getUserId, currentUserId)
                        .eq(LikeRecord::getIsCanceled, 0);
                LikeRecord likeRecord = likeRecord1Mapper.selectOne(lambdaQueryWrapper);
                if (likeRecord != null) {
                    commentListVO.setIsLike(true);
                } else {
                    commentListVO.setIsLike(false);
                }
            }
            //返回数据
            PageLikeVO<CommentListVO> pageLikeVO = new PageLikeVO<>();
            pageLikeVO.setLastId(curentLastId);
            pageLikeVO.setDateList(commentListVOS);
            pageLikeVO.setHasMore(hasmore);
            return Result.ok(pageLikeVO);
        }


    /**获取子评论的列表
     *
     */
    @Override
    public Result getReplyCommentList(ReplyDTO replyDTO) {
        //获取当前用户的id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Long parentId = Long.parseLong(replyDTO.getParentId());
        Integer pageSize=replyDTO.getSize();
        Long curentLastId=null;
        Boolean hasmore=false;
        //判断父评论是否存在
        LambdaQueryWrapper<Comment> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Comment::getId, parentId)
                .eq(Comment::getStatus, 1);
        Comment comment = commentMapper.selectOne(lambdaQueryWrapper);
        if(comment==null)
        {
            return Result.fail("父评论不存在");
        }
        Integer targetType = comment.getTargetType();
        Long targetId = comment.getTargetId();
        //判断目标是否存在
        if (targetType == CommentEnum.MUSIC.getValue()) {
            LambdaQueryWrapper<Music> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(Music::getId, targetId)
                    .eq(Music::getStatus, 1);
            Music music = music2Mapper.selectOne(lambdaQueryWrapper1);
            if (music == null) {
                return Result.fail("目标音乐未上架");
            }
        } else if (targetType == CommentEnum.PLAYLIST.getValue()) {
            LambdaQueryWrapper<Playlist> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(Playlist::getId, targetId)
                    .eq(Playlist::getIsPublic, 1);
            Playlist playlist = playList2Mapper.selectOne(lambdaQueryWrapper1);
            if (playlist == null) {
                return Result.fail("目标歌单未公开");
            }
        } else if (targetType == CommentEnum.ALBUM.getValue()) {
            LambdaQueryWrapper<Album> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(Album::getId, targetId)
                    .eq(Album::getStatus, 1);
            Album album = album2Mapper.selectOne(lambdaQueryWrapper1);
            if (album == null) {
                return Result.fail("目标专辑未上架");
            }
        }
        else if(targetType == CommentEnum.COMMUNITY.getValue()) {
            LambdaQueryWrapper<CommunityPost> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
            lambdaQueryWrapper1.eq(CommunityPost::getId, targetId)
                    .eq(CommunityPost::getIsDeleted, 0);
            CommunityPost communityPost = communityPostMapper.selectOne(lambdaQueryWrapper1);
            if (communityPost == null)
                return Result.fail("目标动态已删除");
        }
            Long lastId = null;
            if (replyDTO.getLastId() != null && !replyDTO.getLastId().isEmpty()) {
                lastId = Long.parseLong(replyDTO.getLastId());
            }
            //查找lastId后面的评论
            List<ReplyCommentListVO> commentListVOS= commentMapper.replyCommentList(parentId, lastId, pageSize);
            if (commentListVOS == null) {
                return Result.ok("后面没有数据了");
            }
            //判断是否还有更多数据
            Long has=commentMapper.hasmore(parentId, lastId);
            hasmore=has>0;
            //记录下一页的lastId
            curentLastId = commentListVOS.get(commentListVOS.size() - 1).getId();
            for (ReplyCommentListVO commentListVO : commentListVOS) {
                if (commentListVO.getUserId() == currentUserId) {
                    commentListVO.setIsSelf(true);
                } else {
                    commentListVO.setIsSelf(false);
                }
                LambdaQueryWrapper<LikeRecord> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                lambdaQueryWrapper1.eq(LikeRecord::getTargetId, commentListVO.getId())
                        .eq(LikeRecord::getTargetType, LikeEnum.COMMENT.getValue())
                        .eq(LikeRecord::getUserId, currentUserId)
                        .eq(LikeRecord::getIsCanceled, 0);
                LikeRecord likeRecord = likeRecord1Mapper.selectOne(lambdaQueryWrapper1);
                if (likeRecord != null) {
                    commentListVO.setIsLike(true);
                } else {
                    commentListVO.setIsLike(false);
                }
        }
        //返回数据
        PageLikeVO<ReplyCommentListVO> pageLikeVO = new PageLikeVO<>();
        pageLikeVO.setLastId(curentLastId);
        pageLikeVO.setDateList(commentListVOS);
        pageLikeVO.setHasMore(hasmore);
        return Result.ok(pageLikeVO);
    }
}
