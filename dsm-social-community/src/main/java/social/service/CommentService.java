package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.*;

@Service
public interface CommentService {
    //发布评论
    Result releaseComment(CommentDTO commentDTO);
    Result getCommentList(CommentListDTO commentListDTO);
    Result replyComment(CommentReplyDTO commentReplyDTO);
    Result getReplyCommentList(ReplyDTO replyDTO);

    Result deleteComment(Long commentId);

    Result reportComment(ReportDTO reportDTO);
}
