package com.future.music.controller;

import entity.dto.FileUploadDTO;
import com.future.music.domain.dto.MusicMvDTO;
import com.future.music.service.IMusicMvService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.MusicMvVO;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@Api(tags = "MV管理")
@RequestMapping("/mv")
@Validated
public class MusicMvController {

    @Autowired
    private IMusicMvService musicMvService;

    /**
     * 上传MV文件
     * @param fileUploadDTO 文件上传参数
     * @return 上传结果
     */
    @ApiOperation(value = "上传MV文件")
    @PostMapping("/uploadMv")
    public Result<String> uploadMv(FileUploadDTO fileUploadDTO, @RequestParam(required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }
        fileUploadDTO.setFile(file);
        
        if (fileUploadDTO.getMd5Value() == null || fileUploadDTO.getMd5Value().isEmpty()) {
            return Result.fail("文件MD5值不能为空");
        }
        if (fileUploadDTO.getChunk() == null) {
            return Result.fail("分片索引不能为空");
        }
        if (fileUploadDTO.getChunks() == null) {
            return Result.fail("总分片数不能为空");
        }
        if (fileUploadDTO.getChunkMd5() == null || fileUploadDTO.getChunkMd5().isEmpty()) {
            return Result.fail("分片MD5值不能为空");
        }
        if (fileUploadDTO.getOperation() == null || fileUploadDTO.getOperation().isEmpty()) {
            return Result.fail("操作类型不能为空");
        }
        
        return musicMvService.uploadMv(fileUploadDTO);
    }

    /**
     * 合并MV分片文件
     * @param fileUploadDTO 文件上传参数
     * @return 合并结果
     */
    @ApiOperation(value = "合并MV分片文件")
    @PostMapping("/merge")
    public Result<String> mergeMvChunks(FileUploadDTO fileUploadDTO) {
        return musicMvService.mergeMvChunks(fileUploadDTO);
    }

    /**
     * 创建MV
     * @param musicMvDTO MV相关参数
     * @return 创建结果
     */
    @ApiOperation(value = "创建MV")
    @PostMapping("/create")
    public Result<Long> createMv(@RequestBody @Validated MusicMvDTO musicMvDTO) {
        return musicMvService.createMv(musicMvDTO);
    }

    /**
     * 更新MV信息
     * @param mvId MV ID
     * @param musicMvDTO MV相关参数
     * @return 更新结果
     */
    @ApiOperation(value = "更新MV信息")
    @PutMapping("/update/{mvId}")
    public Result<Boolean> updateMv(@PathVariable Long mvId, @RequestBody @Validated MusicMvDTO musicMvDTO) {
        return musicMvService.updateMv(mvId, musicMvDTO);
    }

    /**
     * 删除MV
     * @param mvId MV ID
     * @return 删除结果
     */
    @ApiOperation(value = "删除MV")
    @DeleteMapping("/delete/{mvId}")
    public Result<Boolean> deleteMv(@PathVariable Long mvId) {
        return musicMvService.deleteMv(mvId);
    }

    /**
     * 根据MV ID查询MV详情
     * @param mvId MV ID
     * @return MV详情
     */
    @ApiOperation(value = "查询MV详情")
    @GetMapping("/detail/{mvId}")
    public Result<MusicMvVO> getMvDetail(@PathVariable Long mvId) {
        return musicMvService.getMvDetail(mvId);
    }

    /**
     * 查询音乐人的所有MV
     * @param musicianId 音乐人ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
//    @ApiOperation(value = "查询音乐人的所有MV")
//    @GetMapping("/musician/{musicianId}")
//    public Result<PageResp<MusicMvVO>> getMusicianMvs(@PathVariable Long musicianId,
//                                                     @RequestParam(defaultValue = "1") Integer pageNum,
//                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
//        return musicMvService.getMusicianMvs(musicianId, pageNum, pageSize);
//    }

    /**
     * 查询热门MV列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "查询热门MV列表")
    @GetMapping("/hot")
    public Result<PageResp<MusicMvVO>> getHotMvs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getHotMvs(pageNum, pageSize);
    }

    /**
     * 查询最新MV列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "查询最新MV列表")
    @GetMapping("/new")
    public Result<PageResp<MusicMvVO>> getNewMvs(@RequestParam(defaultValue = "1") Integer pageNum, 
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getNewMvs(pageNum, pageSize);
    }

    /**
     * 查询VIP专属MV列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "查询VIP专属MV列表")
    @GetMapping("/vip")
    public Result<PageResp<MusicMvVO>> getVipMvs(@RequestParam(defaultValue = "1") Integer pageNum, 
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getVipMvs(pageNum, pageSize);
    }

    /**
     * 根据时间、收藏量筛选MV
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param minCollectCount 最小收藏量（可选）
     * @param maxCollectCount 最大收藏量（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "根据时间、收藏量筛选MV")
    @GetMapping("/filter")
    public Result<PageResp<MusicMvVO>> filterMvs(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Long minCollectCount,
            @RequestParam(required = false) Long maxCollectCount,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.filterMvs(startTime, endTime, minCollectCount, maxCollectCount, pageNum, pageSize);
    }

    /**
     * 将歌曲添加到指定MV
     * @param mvId MV ID
     * @param musicId 歌曲ID
     * @return 添加结果
     */
    @ApiOperation(value = "将歌曲添加到指定MV")
    @PostMapping("/addMusic/{mvId}")
    public Result<Boolean> addMusicToMv(@PathVariable Long mvId, @RequestParam Long musicId) {
        return musicMvService.addMusicToMv(mvId, musicId);
    }

    /**
     * 将歌曲从其对应的MV中移除
     * @param musicId 歌曲ID
     * @return 移除结果
     */
    @ApiOperation(value = "将歌曲从其对应的MV中移除")
    @DeleteMapping("/removeMusic")
    public Result<Boolean> removeMusicFromMv(@RequestParam Long musicId) {
        return musicMvService.removeMusicFromMv(musicId);
    }

    /**
     * 查询未指定音乐的MV列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "查询未指定音乐的MV列表")
    @GetMapping("/withoutMusic")
    public Result<PageResp<MusicMvVO>> getMvsWithoutMusic(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getMvsWithoutMusic(pageNum, pageSize);
    }

    /**
     * 查询未指定MV的音乐列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "查询未指定MV的音乐列表")
    @GetMapping("/musics/withoutMv")
    public Result<PageResp<entity.pojo.music.Music>> getMusicsWithoutMv(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getMusicsWithoutMv(pageNum, pageSize);
    }

    /**
     * 分页查询我的MV（按创建时间降序）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询我的MV（按创建时间降序）")
    @GetMapping("/myMvs/createTime")
    public Result<PageResp<MusicMvVO>> getMyMvsByCreateTime(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getMyMvsByCreateTime(pageNum, pageSize);
    }

    /**
     * 分页查询我的MV（按收藏量降序）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询我的MV（按收藏量降序）")
    @GetMapping("/myMvs/collectCount")
    public Result<PageResp<MusicMvVO>> getMyMvsByCollectCount(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return musicMvService.getMyMvsByCollectCount(pageNum, pageSize);
    }

}
