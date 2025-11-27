package social.controller;

import entity.pojo.util.Result;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.dto.PageDTO;
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
@Api(tags = "关注 点赞 收藏")
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
    @ApiOperation(value = "关注")
    @GetMapping("/follow")
    public Result follow(@RequestParam(value = "userId",required = true)Long userId)
    {
        return userBaseService.follow(userId);
    }
    @ApiOperation(value = "取消关注")
    @GetMapping("/unfollow")
    public Result unfollow(@RequestParam(value = "userId",required = true)Long userId)
    {
        return userBaseService.unFollow(userId);
    }
    @ApiOperation(value = "获取粉丝列表")
    @PostMapping("/getFollowedList")
    public Result getFollowerList(@Valid @RequestBody PageDTO pageDTO)
    {
        return userBaseService.getFollowerList(pageDTO);
    }
    @ApiOperation(value = "获取粉丝数")
    @GetMapping("/getFollowerCount")
    public Result getFollowerCount()
    {
        return userBaseService.getFollowerCount();
    }
    @ApiOperation(value = "获取关注数")
    @GetMapping("/getFollowCount")
    public Result getFollowCount()
    {
        return userBaseService.getFollowCount();
    }

    @ApiOperation(value = "获取关注的音乐人列表")
    @PostMapping("/getFollowMusician")
    public Result getFollowMusician(@Valid @RequestBody PageDTO pageDTO)
    {
        return userBaseService.getFollowListMusic(pageDTO);
    }
    @ApiOperation(value = "获取关注的普通人列表")
    @PostMapping("/getFollowNormal")
    public Result getFollowNormal(@Valid @RequestBody PageDTO pageDTO)
    {
        return userBaseService.getFollowListNormal(pageDTO);
    }

}
