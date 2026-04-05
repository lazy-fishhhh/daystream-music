package admin.service;

import admin.domain.dto.BannerDTO;
import admin.domain.vo.BannerVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;


import java.util.List;

/**
 * Banner管理Service接口
 * 用于运营管理的轮播图管理业务逻辑
 */
public interface AdminBannerService {

    /**
     * 获取Banner列表（分页）
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return Banner列表分页结果
     */
    Result<PageResp<BannerVO>> getBannerList(Integer page, Integer pageSize);

    /**
     * 获取Banner详情
     * @param id Banner ID
     * @return Banner详情
     */
    Result<BannerVO> getBannerDetail(Long id);

    /**
     * 添加Banner
     * @param bannerDTO Banner信息
     * @return 操作结果
     */
    Result<Void> addBanner(BannerDTO bannerDTO);

    /**
     * 更新Banner
     * @param bannerDTO Banner信息
     * @return 操作结果
     */
    Result<Void> updateBanner(BannerDTO bannerDTO);

    /**
     * 删除Banner
     * @param id Banner ID
     * @return 操作结果
     */
    Result<Void> deleteBanner(Long id);

    /**
     * 批量删除Banner
     * @param ids Banner ID列表
     * @return 操作结果
     */
    Result<Void> batchDeleteBanner(List<Long> ids);

    /**
     * 更新Banner状态
     * @param id Banner ID
     * @param status 状态（0：禁用，1：启用）
     * @return 操作结果
     */
    Result<Void> updateBannerStatus(Long id, Integer status);

    /**
     * 搜索Banner
     * @param page 当前页码
     * @param pageSize 每页条数
     * @param keyword 搜索关键词
     * @return Banner列表分页结果
     */
    Result<PageResp<BannerVO>> searchBanner(Integer page, Integer pageSize, String keyword);
}
