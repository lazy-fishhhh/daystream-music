package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import social.domain.dto.CommunityDTO;
import social.domain.dto.CommunityVOListDTO;
import social.service.CommunityPostService;
import social.service.Minio2Service;

import javax.validation.Valid;

@RestController
@Api(tags = "社区功能")
@RequestMapping("/community")
public class CommunityPostController {
    @Autowired
    private CommunityPostService communityPostService;
    @Autowired
    private Minio2Service minio2Service;
    @ApiOperation(value = "发布动态")
    @PostMapping("/postSocialUpdate")
    public Result postSocial(@Valid @RequestBody CommunityDTO communityDTO) {
        return communityPostService.releasePost(communityDTO);
    }
    @PostMapping("/updateImage")
    @ApiOperation(value = "上传图片")
    public Result updateImage(@NotNull(message = "请上传图片") @RequestParam MultipartFile file) {
        return minio2Service.uploadFile(file);
    }
    @DeleteMapping("/deleteImage")
    @ApiOperation(value = "删除图片")
    public Result deleteImage(@NotNull(message = "请上传图片") @RequestParam String fileUrl) {
        return minio2Service.deleteFile(fileUrl);

    }
    @DeleteMapping("/deletePost")
    @ApiOperation(value = "删除动态")
    public Result deletePost(@NotBlank(message = "动态id不能为空") @RequestParam String postId) {
        return communityPostService.deletePost(postId);
    }
    @PostMapping("/getAllPost")
    @ApiOperation(value = "查询所有动态")
    public Result getAllPost(@Valid @RequestBody CommunityVOListDTO communityVOListDTO) {
        return communityPostService.getPost(communityVOListDTO);
    }
    @GetMapping("/getDetailPost")
    @ApiOperation(value = "查询动态详情")
    public Result getDetailPost(@NotBlank(message = "动态id不能为空") @RequestParam String postId) {
        return communityPostService.getDetail(postId);
    }

}
