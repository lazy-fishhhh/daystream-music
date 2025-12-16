package admin.controller;

import admin.domain.dto.DeletedMusicianDTO;
import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.PostApplyDTO;
import admin.domain.vo.AdminPostVO;
import admin.service.PostApplyService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/communityApply")
@Api(tags = "动态审核管理")
@PreAuthorize("hasRole('管理员')")
public class CommunityApplyController {
    @Autowired
    private PostApplyService postApplyService;
    @PostMapping("/list")
    @ApiOperation(value="获取动态申请列表")
    Result<PageResp<AdminPostVO>> communityApplyList(@Valid @RequestBody PostApplyDTO postApplyDTO) {
        return postApplyService.getList(postApplyDTO);
    }
    @PostMapping("passBatch")
    @ApiOperation(value="批量通过动态")
    Result<String> passBatch(@RequestBody @Valid DeletedMusicianDTO ids) {
        return postApplyService.passBatch(ids);
    }
    @PostMapping("rejectBatch")
    @ApiOperation(value="批量驳回动态")
    Result<String> rejectBatch(@RequestBody @Valid DeletedMusicianDTO ids) {
        return postApplyService.rejectBatch(ids);
    }
    @PostMapping("handle")
    @ApiOperation(value="处理动态")
    Result<String> handle(@RequestBody @Valid HandleReportDTO handleDto) {
        return postApplyService.handle(handleDto);
    }
}
