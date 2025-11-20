package social.controller;

import entity.pojo.util.Result;
import io.swagger.annotations.ApiResponse;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.dto.PageDTO;
import social.domain.vo.CollectionVO;
import social.domain.vo.LikeMusicVO;
import social.domain.vo.LikeVO;
import social.service.UserBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/userbase")
@Api(tags = "用户基础社交功能")
public class UserBaseController {
    @Autowired
    private UserBaseService userBaseService;
    @ApiOperation(value = "点赞/取消点赞")
    @PostMapping("/likerecord")
    public Result<LikeVO> likerecord(@Valid @RequestBody LikeDTO likeDTO)
    {
        return userBaseService.likeandUnlike(likeDTO);
    }
    @ApiOperation(value = "收藏/取消收藏")
    @PostMapping("/collect")
    public Result<CollectionVO> collection(@Valid @RequestBody CollectionDTO collectionDTO)
    {
        return userBaseService.collectUnCollect(collectionDTO);
    }
    @ApiOperation(value = "获取用户所有点赞的音乐")
    @GetMapping("/getLikeMusic")
    public Result getLikeMusic(@Valid @RequestBody PageDTO pageDTO)
    {
        return userBaseService.getLikeMusic(pageDTO);
    }
    @ApiOperation(value = "获取用户收藏的专辑")
    @GetMapping("/getAlbums")
    public Result getAlbums(@Valid @RequestBody PageDTO pageDTO)
    {
        return userBaseService.getCollectAlbum(pageDTO);
    }
    @ApiOperation(value = "获取用户收藏的歌单")
    @GetMapping("/getPlayList")
    public Result getPlayList(@Valid @RequestBody PageDTO pageDTO)
    {
        return userBaseService.getPlayList(pageDTO);
    }
}
