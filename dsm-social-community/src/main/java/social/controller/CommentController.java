package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import social.domain.dto.CommentDTO;
import social.domain.dto.CommentListDTO;
import social.domain.dto.CommentReplyDTO;
import social.domain.dto.ReplyDTO;
import social.service.CommentService;

@RestController
@Api(tags = "评论功能")
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @ApiOperation(value = "发表评论")
    @PostMapping("/releaseComment")
    public Result releaseComment(@Valid @RequestBody CommentDTO commentDTO) {
        return commentService.releaseComment(commentDTO);
    }

    @ApiOperation(value = "获取父评论列表")
    @PostMapping("/getCommentList")
    public Result getCommentList(@Valid @RequestBody CommentListDTO commentListDTO) {
        return commentService.getCommentList(commentListDTO);
    }
    @ApiOperation(value = "获取子评论列表")
    @PostMapping("/getReplyCommentList")
    public Result getReplyCommentList(@Valid @RequestBody ReplyDTO replyDTO) {
        return commentService.getReplyCommentList(replyDTO);
    }

    @ApiOperation(value = "回复评论")
    @PostMapping("/replyComment")
    public Result replyComment(@Valid @RequestBody CommentReplyDTO commentReplyDTO) {
        return commentService.replyComment(commentReplyDTO);
    }
    @ApiOperation(value = "删除评论",notes = "评论者可以删除自己的评论，评论歌曲/歌单/动态/专辑作者可以删除发布在自己作品下的评论")
    @DeleteMapping("/deleteComment")
    public Result deleteComment(@RequestParam @NotNull(message = "评论id不能为空") Long commentId) {
        return commentService.deleteComment(commentId);
    }
}
