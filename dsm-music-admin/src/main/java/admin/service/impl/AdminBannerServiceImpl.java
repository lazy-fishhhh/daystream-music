package admin.service.impl;

import admin.domain.dto.BannerDTO;
import admin.domain.vo.BannerVO;
import admin.mapper.AdminBannerMapper;
import admin.service.AdminBannerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.Banner;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Banner管理Service实现类
 * 用于运营管理的轮播图管理业务逻辑实现
 */
@Service
@Transactional
public class AdminBannerServiceImpl implements AdminBannerService {

    @Autowired
    private AdminBannerMapper adminBannerMapper;


    public PageResp<BannerVO> convertPageToResp(Page<Banner> page){
        PageResp<BannerVO> pageResp = new PageResp<>();
        pageResp.setTotal(page.getTotal());
        pageResp.setPages(page.getPages());
        pageResp.setCurrent(page.getCurrent());
        return pageResp;
    }

    @Override
    public Result<PageResp<BannerVO>> getBannerList(Integer page, Integer pageSize) {
        Page<Banner> bannerPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Banner::getSortOrder)
                    .orderByDesc(Banner::getCreateTime);
        Page<Banner> resultPage = adminBannerMapper.selectPage(bannerPage, queryWrapper);
        
        // 将Banner转换为BannerVO
        List<BannerVO> bannerVOList = resultPage.getRecords().stream()
                .map(BannerVO::fromBanner)
                .collect(Collectors.toList());
        
        PageResp<BannerVO> pageResp = convertPageToResp(resultPage);
        // 设置记录列表
        pageResp.setRecords(bannerVOList);
        
        return Result.ok(pageResp);
    }

    @Override
    public Result<BannerVO> getBannerDetail(Long id) {
        Banner banner = adminBannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail("Banner不存在");
        }
        
        // 转换为BannerVO
        BannerVO bannerVO = BannerVO.fromBanner(banner);
        
        return Result.ok(bannerVO);
    }

    @Override
    public Result<Void> addBanner(BannerDTO bannerDTO) {
        // 验证actionType和targetId的关系
        if (bannerDTO.getActionType() != null && bannerDTO.getActionType().getValue() != 0) {
            if (bannerDTO.getTargetId() == null) {
                return Result.fail("当交互行为类型不为0时，目标ID不能为空");
            }
        } else {
            // 当actionType为0或null时，targetId应该为null
            bannerDTO.setTargetId(null);
        }
        
        Banner banner = new Banner();
        // 单独处理actionType的转换
        banner.setActionType(bannerDTO.getActionType() != null ? bannerDTO.getActionType().getValue() : null);
        // 手动复制其他属性
        banner.setTitle(bannerDTO.getTitle());
        banner.setImageUrl(bannerDTO.getImageUrl());
        banner.setLinkUrl(bannerDTO.getLinkUrl());
        banner.setSortOrder(bannerDTO.getSortOrder());
        banner.setStatus(bannerDTO.getStatus());
        banner.setTargetId(bannerDTO.getTargetId());
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        adminBannerMapper.insert(banner);
        return Result.ok();
    }

    @Override
    public Result<Void> updateBanner(BannerDTO bannerDTO) {
        Banner existingBanner = adminBannerMapper.selectById(bannerDTO.getId());
        if (existingBanner == null) {
            return Result.fail("Banner不存在");
        }
        
        // 验证actionType和targetId的关系
        if (bannerDTO.getActionType() != null && bannerDTO.getActionType().getValue() != 0) {
            if (bannerDTO.getTargetId() == null) {
                return Result.fail("当交互行为类型不为0时，目标ID不能为空");
            }
        } else {
            // 当actionType为0或null时，targetId应该为null
            bannerDTO.setTargetId(null);
        }
        
        // 单独处理actionType的转换
        existingBanner.setActionType(bannerDTO.getActionType() != null ? bannerDTO.getActionType().getValue() : null);
        // 手动复制其他属性
        existingBanner.setTitle(bannerDTO.getTitle());
        existingBanner.setImageUrl(bannerDTO.getImageUrl());
        existingBanner.setLinkUrl(bannerDTO.getLinkUrl());
        existingBanner.setSortOrder(bannerDTO.getSortOrder());
        existingBanner.setStatus(bannerDTO.getStatus());
        existingBanner.setTargetId(bannerDTO.getTargetId());
        existingBanner.setUpdateTime(LocalDateTime.now());
        adminBannerMapper.updateById(existingBanner);
        return Result.ok();
    }

    @Override
    public Result<Void> deleteBanner(Long id) {
        int result = adminBannerMapper.deleteById(id);
        if (result == 0) {
            return Result.fail("Banner不存在");
        }
        return Result.ok();
    }

    @Override
    public Result<Void> batchDeleteBanner(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的Banner");
        }
        adminBannerMapper.deleteBatchIds(ids);
        return Result.ok();
    }

    @Override
    public Result<Void> updateBannerStatus(Long id, Integer status) {
        Banner banner = adminBannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail("Banner不存在");
        }
        banner.setStatus(status);
        banner.setUpdateTime(LocalDateTime.now());
        adminBannerMapper.updateById(banner);
        return Result.ok();
    }

    @Override
    public Result<PageResp<BannerVO>> searchBanner(Integer page, Integer pageSize, String keyword) {
        Page<Banner> bannerPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果关键词不为空，添加搜索条件
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Banner::getTitle, keyword)
                   .or().like(Banner::getLinkUrl, keyword);
        }
        
        // 保持与getBannerList相同的排序逻辑
        queryWrapper.orderByDesc(Banner::getSortOrder)
                    .orderByDesc(Banner::getCreateTime);
        
        Page<Banner> resultPage = adminBannerMapper.selectPage(bannerPage, queryWrapper);
        
        // 将Banner转换为BannerVO
        List<BannerVO> bannerVOList = resultPage.getRecords().stream()
                .map(BannerVO::fromBanner)
                .collect(Collectors.toList());
        
        PageResp<BannerVO> pageResp = convertPageToResp(resultPage);
        // 设置记录列表
        pageResp.setRecords(bannerVOList);
        
        return Result.ok(pageResp);
    }
}
