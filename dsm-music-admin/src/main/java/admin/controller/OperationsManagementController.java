package admin.controller;

import admin.domain.dto.BannerDTO;
import admin.domain.vo.BannerVO;
import admin.service.AdminBannerService;
import admin.service.AdminRecommendPositionService;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/operations")
@Api(tags = "运营管理")
@Validated
public class OperationsManagementController {

    @Autowired
    private AdminBannerService adminBannerService;

    @Autowired
    private AdminRecommendPositionService adminRecommendPositionService;




    // ==================== Banner管理接口 ====================

    @ApiOperation("获取Banner列表")
    @GetMapping("/banner/list")
    public Result<PageResp<BannerVO>> getBannerList(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer pageSize) {
        return adminBannerService.getBannerList(page, pageSize);
    }

    @ApiOperation("搜索Banner")
    @GetMapping("/banner/search")
    public Result<PageResp<BannerVO>> searchBanner(@RequestParam(defaultValue = "1") Integer page, 
                                                  @RequestParam(defaultValue = "10") Integer pageSize, 
                                                  @RequestParam(required = false) String keyword) {
        return adminBannerService.searchBanner(page, pageSize, keyword);
    }

    @ApiOperation("获取Banner详情")
    @GetMapping("/banner/{id}")
    public Result<BannerVO> getBannerDetail(@PathVariable Long id) {
        return adminBannerService.getBannerDetail(id);
    }

    @ApiOperation("添加Banner")
    @PostMapping("/banner")
    public Result<Void> addBanner(@RequestBody BannerDTO bannerDTO) {
        return adminBannerService.addBanner(bannerDTO);
    }

    @ApiOperation("更新Banner")
    @PutMapping("/banner")
    public Result<Void> updateBanner(@RequestBody BannerDTO bannerDTO) {
        return adminBannerService.updateBanner(bannerDTO);
    }

    @ApiOperation("删除Banner")
    @DeleteMapping("/banner/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        return adminBannerService.deleteBanner(id);
    }

    @ApiOperation("批量删除Banner")
    @DeleteMapping("/banner/batch")
    public Result<Void> batchDeleteBanner(@RequestBody List<Long> ids) {
        return adminBannerService.batchDeleteBanner(ids);
    }

    @ApiOperation("更新Banner状态")
    @PutMapping("/banner/status/{id}")
    public Result<Void> updateBannerStatus(@PathVariable Long id, @RequestParam Integer status) {
        return adminBannerService.updateBannerStatus(id, status);
    }

    // ==================== 推荐位管理接口 ====================

//    @ApiOperation("获取推荐位列表")
//    @GetMapping("/recommend/position/list")
//    public Result<PageResp<RecommendPosition>> getRecommendPositionList(
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "10") Integer pageSize,
//            @RequestParam(required = false) Integer type) {
//        return adminRecommendPositionService.getRecommendPositionList(page, pageSize, type);
//    }
//
//    @ApiOperation("获取推荐位详情")
//    @GetMapping("/recommend/position/{id}")
//    public Result<RecommendPosition> getRecommendPositionDetail(@PathVariable Long id) {
//        return adminRecommendPositionService.getRecommendPositionDetail(id);
//    }
//
//    @ApiOperation("添加推荐位")
//    @PostMapping("/recommend/position")
//    public Result<Void> addRecommendPosition(@RequestBody RecommendPositionDTO recommendPositionDTO) {
//        return adminRecommendPositionService.addRecommendPosition(recommendPositionDTO);
//    }
//
//    @ApiOperation("更新推荐位")
//    @PutMapping("/recommend/position")
//    public Result<Void> updateRecommendPosition(@RequestBody RecommendPositionDTO recommendPositionDTO) {
//        return adminRecommendPositionService.updateRecommendPosition(recommendPositionDTO);
//    }
//
//    @ApiOperation("删除推荐位")
//    @DeleteMapping("/recommend/position/{id}")
//    public Result<Void> deleteRecommendPosition(@PathVariable Long id) {
//        return adminRecommendPositionService.deleteRecommendPosition(id);
//    }
//
//    @ApiOperation("批量删除推荐位")
//    @DeleteMapping("/recommend/position/batch")
//    public Result<Void> batchDeleteRecommendPosition(@RequestBody List<Long> ids) {
//        return adminRecommendPositionService.batchDeleteRecommendPosition(ids);
//    }
//
//    @ApiOperation("更新推荐位状态")
//    @PutMapping("/recommend/position/status/{id}")
//    public Result<Void> updateRecommendPositionStatus(@PathVariable Long id, @RequestParam Integer status) {
//        return adminRecommendPositionService.updateRecommendPositionStatus(id, status);
//    }
//
//    @ApiOperation("获取启用的推荐位")
//    @GetMapping("/recommend/position/enabled")
//    public Result<List<RecommendPosition>> getEnabledRecommendPositions(@RequestParam Integer type) {
//        return adminRecommendPositionService.getEnabledRecommendPositions(type);
//    }
}
