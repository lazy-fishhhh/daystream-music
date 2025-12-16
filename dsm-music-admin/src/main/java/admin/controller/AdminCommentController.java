package admin.controller;

import admin.domain.dto.AdminCommentDTO;
import admin.domain.dto.CommentBatchDTO;
import admin.domain.dto.UpdateCommentDTO;
import admin.domain.vo.AdminCommentVO;
import admin.service.AdminCommentService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Admincomment")
@Api(tags = "评论管理")
@PreAuthorize("hasRole('管理员')")
public class AdminCommentController {
    @Autowired
    private AdminCommentService adminCommentService;
    @PostMapping("/list")
    @ApiOperation(value = "获取评论列表")
    public Result<PageResp<AdminCommentVO>> getCommentList(@RequestBody AdminCommentDTO commentDTO) {
        return adminCommentService.getCommentList(commentDTO);
    }
    @GetMapping("detailComment")
    @ApiOperation(value = "获取评论详情")
    public Result<AdminCommentVO> getCommentDetail(@NotNull(message = "id不能为空") @RequestParam(value = "id",required = true) Long id) {
        return adminCommentService.getCommentDetail(id);
    }
    @PostMapping("/deleteComment")
    @ApiOperation(value = "批量删除评论")
    public Result<String> deleteComment(@Valid @RequestBody CommentBatchDTO commentBatchDTO) {
        return adminCommentService.deleteComment(commentBatchDTO);
    }
    @PostMapping("/updateComment")
    @ApiOperation(value = "更新评论")
    public Result<String> updateComment(@Valid @RequestBody UpdateCommentDTO updateCommentDTO) {
        return adminCommentService.updateComment(updateCommentDTO);

    }
}
