package admin.controller;

import admin.domain.dto.MusicAuditDTO;
import admin.domain.dto.MusicListDTO;
import admin.domain.dto.MvAuditDTO;
import admin.domain.dto.MvListDTO;
import admin.domain.vo.MusicVO;
import admin.service.AdminMusicService;
import admin.service.AdminMusicMvService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.MusicMvVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 音乐作品审核控制器
 */
@RestController
@RequestMapping("admin")
//@PreAuthorize("hasRole('管理员')")
@Api(tags = "音乐作品审核管理")
public class MasterPieceController {

    @Autowired
    private AdminMusicService adminMusicService;
    
    @Autowired
    private AdminMusicMvService adminMusicMvService;

    /**
     * 获取音乐作品审核列表
     * @param musicListDTO 音乐作品查询条件
     * @return 分页的音乐作品列表
     */
    @ApiOperation(value = "获取音乐作品审核列表")
    @PostMapping("/getMasterPieceList")
    public Result<PageResp<MusicVO>> getMasterPieceList(@Valid @RequestBody MusicListDTO musicListDTO) {
        return adminMusicService.getMusicAuditList(musicListDTO);
    }

    @ApiOperation(value = "查询单条音乐审核详情")
    @PostMapping("/getMasterPieceDetail")
    public Result<MusicVO> getMasterPieceDetail(@RequestParam Long musicId) {
        return adminMusicService.getMusicAuditDetail(musicId);
    }

    /**
     * 音乐作品审核
     * @param musicAuditDTO 审核信息
     * @return 审核结果
     */
    @ApiOperation(value = "音乐作品审核")
    @PostMapping("/auditMasterPiece")
    public Result<String> auditMasterPiece(@Valid @RequestBody MusicAuditDTO musicAuditDTO) {
        return adminMusicService.auditMusic(musicAuditDTO);
    }

    /**
     * 批量通过音乐作品审核
     * @param musicAuditDTO 批量审核信息
     * @return 审核结果
     */
    @ApiOperation(value = "批量通过音乐作品审核")
    @PostMapping("/batchPassMasterPiece")
    public Result<String> batchPassMasterPiece(@Valid @RequestBody MusicAuditDTO musicAuditDTO) {
        return adminMusicService.batchPass(musicAuditDTO);
    }

    /**
     * 批量驳回音乐作品审核
     * @param musicAuditDTO 批量审核信息
     * @return 审核结果
     */
    @ApiOperation(value = "批量驳回音乐作品审核")
    @PostMapping("/batchRefuseMasterPiece")
    public Result<String> batchRefuseMasterPiece(@Valid @RequestBody MusicAuditDTO musicAuditDTO) {
        return adminMusicService.batchRefuse(musicAuditDTO);
    }









    
    /**
     * 获取MV审核列表
     * @param mvListDTO MV查询条件
     * @return 分页的MV列表
     */
    @ApiOperation(value = "获取MV审核列表")
    @PostMapping("/getMvAuditList")
    public Result<PageResp<MusicMvVO>> getMvAuditList(@Valid @RequestBody MvListDTO mvListDTO) {
        return adminMusicMvService.getMvAuditList(mvListDTO);
    }
    
//    @ApiOperation(value = "查询单条MV审核详情")
//    @PostMapping("/getMvAuditDetail")
//    public Result<MusicMvVO> getMvAuditDetail(@RequestParam Long mvId) {
//        return adminMusicMvService.getMvAuditDetail(mvId);
//    }
    
    /**
     * MV审核
     * @param mvAuditDTO 审核信息
     * @return 审核结果
     */
    @ApiOperation(value = "MV审核")
    @PostMapping("/auditMv")
    public Result<String> auditMv(@Valid @RequestBody MvAuditDTO mvAuditDTO) {
        return adminMusicMvService.auditMv(mvAuditDTO);
    }
    
    /**
     * 批量通过MV审核
     * @param mvAuditDTO 批量审核信息
     * @return 审核结果
     */
    @ApiOperation(value = "批量通过MV审核")
    @PostMapping("/batchPassMv")
    public Result<String> batchPassMv(@Valid @RequestBody MvAuditDTO mvAuditDTO) {
        return adminMusicMvService.batchPassMv(mvAuditDTO);
    }
    
    /**
     * 批量驳回MV审核
     * @param mvAuditDTO 批量审核信息
     * @return 审核结果
     */
    @ApiOperation(value = "批量驳回MV审核")
    @PostMapping("/batchRefuseMv")
    public Result<String> batchRefuseMv(@Valid @RequestBody MvAuditDTO mvAuditDTO) {
        return adminMusicMvService.batchRefuseMv(mvAuditDTO);
    }



    





}
