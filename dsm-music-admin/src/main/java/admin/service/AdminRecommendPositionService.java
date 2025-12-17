package admin.service;

import admin.domain.dto.RecommendPositionDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.RecommendPosition;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;


import java.util.List;

/**
 * 推荐位管理Service接口
 * 用于运营管理的5歌单30歌曲推荐位管理业务逻辑
 */
public interface AdminRecommendPositionService {

    /**
     * 获取推荐位列表（分页）
     * @param page 当前页码
     * @param pageSize 每页条数
     * @param type 推荐位类型（1：歌单推荐，2：歌曲推荐）
     * @return 推荐位列表分页结果
     */
    Result<PageResp<RecommendPosition>> getRecommendPositionList(Integer page, Integer pageSize, Integer type);

    /**
     * 获取推荐位详情
     * @param id 推荐位ID
     * @return 推荐位详情
     */
    Result<RecommendPosition> getRecommendPositionDetail(Long id);

    /**
     * 添加推荐位
     * @param recommendPositionDTO 推荐位信息
     * @return 操作结果
     */
    Result<Void> addRecommendPosition(RecommendPositionDTO recommendPositionDTO);

    /**
     * 更新推荐位
     * @param recommendPositionDTO 推荐位信息
     * @return 操作结果
     */
    Result<Void> updateRecommendPosition(RecommendPositionDTO recommendPositionDTO);

    /**
     * 删除推荐位
     * @param id 推荐位ID
     * @return 操作结果
     */
    Result<Void> deleteRecommendPosition(Long id);

    /**
     * 批量删除推荐位
     * @param ids 推荐位ID列表
     * @return 操作结果
     */
    Result<Void> batchDeleteRecommendPosition(List<Long> ids);

    /**
     * 更新推荐位状态
     * @param id 推荐位ID
     * @param status 状态（0：禁用，1：启用）
     * @return 操作结果
     */
    Result<Void> updateRecommendPositionStatus(Long id, Integer status);

    /**
     * 获取启用的推荐位（按位置排序）
     * @param type 推荐位类型（1：歌单推荐，2：歌曲推荐）
     * @return 启用的推荐位列表
     */
    Result<List<RecommendPosition>> getEnabledRecommendPositions(Integer type);
}
