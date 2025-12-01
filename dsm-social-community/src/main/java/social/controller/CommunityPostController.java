package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import social.domain.dto.CommunityDTO;
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
    @ApiOperation(value = "上传图片/视频")
    public Result updateImage(@NotNull(message = "请上传图片/视频") @RequestParam MultipartFile file) {
        return minio2Service.uploadFile(file);
    }

}
