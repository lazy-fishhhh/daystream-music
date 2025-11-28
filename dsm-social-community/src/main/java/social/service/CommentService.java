package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.CommentDTO;
import social.domain.dto.CommentListDTO;
import social.domain.dto.CommentReplyDTO;
import social.domain.dto.ReplyDTO;

@Service
public interface CommentService {
    //发布评论
    Result releaseComment(CommentDTO commentDTO);
    Result getCommentList(CommentListDTO commentListDTO);
    Result replyComment(CommentReplyDTO commentReplyDTO);
    Result getReplyCommentList(ReplyDTO replyDTO);

    Result deleteComment(Long commentId);
}
