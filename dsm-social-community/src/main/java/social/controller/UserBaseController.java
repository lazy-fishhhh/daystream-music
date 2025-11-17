package social.controller;

import entity.pojo.util.Result;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.vo.CollectionVO;
import social.domain.vo.LikeVO;
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
    @ApiOperation(value = "点赞/取消点赞",notes = "点赞和取消点赞一个接口，点赞之后再点击就取消点赞")
    @PostMapping("/likerecord")
    public Result<LikeVO> likerecord(@Valid @RequestBody LikeDTO likeDTO)
    {
        return userBaseService.likeandUnlike(likeDTO);
    }
    @ApiOperation(value = "收藏/取消收藏",notes = "收藏和取消收藏一个接口，收藏之后再点击就取消收藏")
    @PostMapping("/collect")
    public Result<CollectionVO> collection(@Valid @RequestBody CollectionDTO collectionDTO)
    {
        return userBaseService.collectUnCollect(collectionDTO);
    }
}
