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
        fileUploadDTO.setFile(file);
        return musicMvService.uploadMv(fileUploadDTO);
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

}
