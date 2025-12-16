package admin.controller;

import admin.domain.dto.AdminPostDTO;
import admin.domain.dto.DeletedMusicianDTO;
import admin.domain.dto.UpdatePostDTO;
import admin.domain.vo.AdminPostVO;
import admin.service.SocialService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admincommunity")
@Api(tags = "动态管理")
@PreAuthorize("hasRole('管理员')")
public class CommunityController {
    @Autowired
    private SocialService communityService;
    @GetMapping("/deleted")
    @ApiOperation(value="删除动态")
    Result<String> deleteCommunity(@NotBlank(message = "动态id不能为空")@RequestParam(value = "id",required = true) String id) {
            return communityService.deletedCommunity(id);
    }
    @PostMapping("/list")
    @ApiOperation(value="获取动态列表")
    Result<PageResp<AdminPostVO>> communityList(@RequestBody @Valid AdminPostDTO adminPostDTO) {
        return communityService.communityList(adminPostDTO);
    }
    @PostMapping("/deleteBatch")
    @ApiOperation(value="批量删除动态")
    Result<String> deleteCommunityBatch(@RequestBody @Valid DeletedMusicianDTO DeletedMusicianDTO) {
        return communityService.deletedCommunityBatch(DeletedMusicianDTO);
    }
    @GetMapping("/banPost")
    @ApiOperation(value="封禁/解封动态")
    Result<String> banPost(@NotBlank(message = "动态id不能为空")@RequestParam(value = "id",required = true) String id) {
        return communityService.banPost(id);
    }
    @GetMapping("/detailPost")
    @ApiOperation(value="获取动态详情")
    Result<AdminPostVO> detailPost(@NotBlank(message = "动态id不能为空")@RequestParam(value = "id",required = true) String id) {
        return communityService.detailPost(id);
    }
    @PostMapping("/updatePost")
    @ApiOperation(value="修改动态")
    Result<String> updatePost(@RequestBody @Valid UpdatePostDTO updatePostDTO) {
        return communityService.updatePost(updatePostDTO);
    }
}
