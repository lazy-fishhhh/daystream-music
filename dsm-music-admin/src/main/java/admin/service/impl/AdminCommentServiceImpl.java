package admin.service.impl;

import admin.domain.dto.AdminCommentDTO;
import admin.domain.dto.CommentBatchDTO;
import admin.domain.dto.DeleteUserDTO;
import admin.domain.dto.UpdateCommentDTO;
import admin.domain.vo.AdminCommentVO;
import admin.mapper.AdminCommentMapper;
import admin.service.AdminCommentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class AdminCommentServiceImpl implements AdminCommentService {
    @Autowired
    private AdminCommentMapper commentMapper;
    @Override
    public Result<PageResp<AdminCommentVO>> getCommentList(AdminCommentDTO commentDTO) {
        Page<AdminCommentVO> page = new Page<>(commentDTO.getPageNum(), commentDTO.getPageSize());
        IPage<AdminCommentVO> iPage = commentMapper.getCommentList(page, commentDTO.getType(), commentDTO.getKeyword());
        PageResp<AdminCommentVO> pageResp = new PageResp<>();
        pageResp.setRecords(iPage.getRecords());
        pageResp.setTotal(iPage.getTotal());
        pageResp.setSize(iPage.getSize());
        pageResp.setPages(iPage.getPages());
        pageResp.setCurrent(iPage.getCurrent());
        return Result.ok(pageResp);
    }

    @Override
    public Result<AdminCommentVO> getCommentDetail(Long id) {
        AdminCommentVO detail = commentMapper.getDetail(id);
        if(detail != null) {
            return Result.ok(detail);
        }
        else{
            return Result.fail("该评论不存在");
        }
    }

    @Override
    public Result<String> deleteComment(CommentBatchDTO commentBatchDTO) {
        List<Long> ids= commentBatchDTO.getCommentId();
        //批量删除评论
        ids.stream()
                .forEach(id->{
                    commentMapper.deleteComment(id);
                });
        return Result.ok("批量删除成功");
    }

    @Override
    public Result<String> updateComment(UpdateCommentDTO updateCommentDTO) {
        Long id=updateCommentDTO.getId();
        return commentMapper.updateComment(id,updateCommentDTO.getContent())>0?Result.ok("修改成功"):Result.fail("修改失败");
    }

}
