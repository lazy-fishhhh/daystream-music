package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import social.domain.dto.LikeDTO;
import social.service.UserBaseService;

@RestController
@RequestMapping("/userbase")
@Api(tags = "用户基础社交功能")
public class UserBaseController {
    @Autowired
    private UserBaseService userBaseService;
    @ApiOperation("点赞/取消点赞")
    @PostMapping("/likerecord")
    public Result likerecord(@Valid @RequestBody LikeDTO likeDTO)
    {
        return userBaseService.likeandUnlike(likeDTO);
    }
}
