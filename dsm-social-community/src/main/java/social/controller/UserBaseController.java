package social.controller;

import entity.pojo.util.Result;
import social.domain.dto.LikeDTO;
import social.service.UserBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userbase")
@Api(tags = "用户基础社交功能")
public class UserBaseController {
    @Autowired
    private UserBaseService userBaseService;
    @ApiOperation(value = "点赞/取消点赞",notes = "点赞和取消点赞一个接口，点赞之后再点击就取消点赞，返回用户是否点赞以及点赞数，islike中0 代表未点赞，1代表已点赞")
    @PostMapping("/likerecord")
    public Result likerecord(@Valid @RequestBody LikeDTO likeDTO)
    {
        return userBaseService.likeandUnlike(likeDTO);
    }
}
