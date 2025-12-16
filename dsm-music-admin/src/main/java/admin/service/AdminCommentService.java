package admin.service;

import admin.domain.dto.AdminCommentDTO;
import admin.domain.dto.CommentBatchDTO;
import admin.domain.dto.DeleteUserDTO;
import admin.domain.dto.UpdateCommentDTO;
import admin.domain.vo.AdminCommentVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;


public interface AdminCommentService {
    Result<PageResp<AdminCommentVO>> getCommentList(AdminCommentDTO commentDTO);

    Result<AdminCommentVO> getCommentDetail(Long id);

    Result<String> deleteComment(CommentBatchDTO commentBatchDTO);

    Result<String> updateComment(UpdateCommentDTO updateCommentDTO);
}
