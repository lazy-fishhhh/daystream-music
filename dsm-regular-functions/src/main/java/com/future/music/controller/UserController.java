package com.future.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.future.login.exception.BusinessException;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.PlaylistDetailVO;
import com.future.music.domain.vo.PlaylistVO;
import com.future.music.domain.vo.SearchHistoryVO;
import com.future.music.mapper.SearchHistoryMapper;
import com.future.music.service.IUserService;
import entity.dto.PlayListDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.Playlist;
import entity.pojo.search.SearchHistory;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    //构造方法注入
    private final IUserService userService;

    @ApiOperation(value = "创建歌单", notes = "不需要传id")
    @PostMapping("/playList/createPlayList")
    public Result<Void> createPlayList(@Validated @RequestBody PlayListDTO playListDTO) {
        userService.createPlayList(playListDTO);
        return Result.ok();
    }


    @ApiOperation("修改歌单")
    @PostMapping("/playList/updatePlayList")
    public Result<Void> updatePlayList(@Validated @RequestBody PlayListDTO playList) {
        // 若@Validated校验失败，会自动抛出MethodArgumentNotValidException异常
        userService.updatePlayList(playList);
        return Result.ok();
    }

    @ApiOperation("获取所有歌单")
    @PostMapping("/playList/getAllPlayList")
    //@Validated 立即验证请求参数是否符合pojo中指定的验证规则，如@NotNull
    //@Validated 可以用在类级别或方法级别，用于验证请求参数是否符合指定的验证规则
    //@Validated 可以用在方法参数中，用于验证请求参数是否符合指定的验证规则
    //@Validated 可以用在方法返回值中，用于验证返回值是否符合指定的验证规则
    public Result<PageResp<Playlist>> getPlayList(@Validated PageReq pageReq) {
        // 若@Validated校验失败，会自动抛出MethodArgumentNotValidException异常
        PageResp<Playlist> playList = userService.getPlayList(pageReq);
        return Result.ok(playList);
    }

    @ApiOperation("删除歌单")
    @PostMapping("/playList/deletePlayList")
    public Result<Void> deletePlayList(long playListId) {
        userService.deletePlayList(playListId);
        return Result.ok();
    }

    @ApiOperation("签到功能")
    @GetMapping("/checkIn")
    public Result<String> checkIn() {
        return userService.checkIn();
    }

    @ApiOperation(value = "获取连续签到天数", notes = "从当前日期开始计算（不包含当天），返回连续签到的天数")
    @GetMapping("/checkIn/getContinuousSignInDays")
    public Result<Integer> getContinuousSignInDays() {
        return userService.getContinuousSignInDays();
    }

    @ApiOperation(value = "搜索")
    @PostMapping("/search")
    public Result<PageResp<?>> search(@Validated @RequestBody SearchDTO searchDTO) {
        return userService.search(searchDTO);
    }

    @ApiOperation(value = "获取搜索历史")
    @GetMapping("/search/getSearchHistory")
    public Result<List<SearchHistoryVO>> getSearchHistory() {
        return userService.getSearchHistory();
    }

    @ApiOperation(value = "删除搜索历史")
    @DeleteMapping("/search/history/{searchHistoryId}")
    public Result<Void> deleteSearchHistory(@PathVariable Long searchHistoryId) {
        // 1. 业务规则校验：ID必须大于0
        if (searchHistoryId == null || searchHistoryId <= 0) {
            return Result.fail("搜索历史ID无效");
        }

        // 2. 校验记录是否存在（避免删除不存在的数据）
        SearchHistory searchHistory = searchHistoryMapper.selectById(searchHistoryId);
        if (searchHistory == null) {
            return Result.fail("搜索历史记录不存在");
        }

        // 3. 执行删除操作
        try {
            if (searchHistoryMapper.deleteById(searchHistoryId) <= 0) {
                return Result.fail("删除搜索历史失败，请稍后重试");
            }
        } catch (Exception e) {
            return Result.fail("删除搜索历史失败，请稍后重试");
        }

        return Result.ok();
    }

    @ApiOperation(value = "删除所有搜索历史")
    @DeleteMapping("/search/history/all")
    public Result<Void> deleteAllSearchHistory() {
        // 1. 执行删除操作
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            QueryWrapper<SearchHistory> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            // 2. 校验是否存在搜索历史记录
            if (searchHistoryMapper.selectCount(queryWrapper) <= 0) {
                return Result.fail("没有搜索历史记录可删除");
            }
            // 3. 执行删除操作
            if (searchHistoryMapper.delete(queryWrapper) <= 0) {
                return Result.fail("删除所有搜索历史失败，请稍后重试");
            }
        } catch (Exception e) {
            return Result.fail("删除所有搜索历史失败，请稍后重试");
        }

        return Result.ok();
    }

}
