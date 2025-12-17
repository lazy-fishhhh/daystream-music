package admin.service.impl;

import admin.domain.dto.RecommendPositionDTO;
import admin.mapper.AdminRecommendPositionMapper;
import admin.service.AdminRecommendPositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.RecommendPosition;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐位管理Service实现类
 * 用于运营管理的5歌单30歌曲推荐位管理业务逻辑实现
 */
@Service
@Transactional
public class AdminRecommendPositionServiceImpl implements AdminRecommendPositionService {

    @Autowired
    private AdminRecommendPositionMapper adminRecommendPositionMapper;

    public PageResp<RecommendPosition> convertPageToResp(Page<RecommendPosition> page) {
        PageResp<RecommendPosition> pageResp = new PageResp<>();
        pageResp.setRecords(page.getRecords());
        pageResp.setTotal(page.getTotal());
        pageResp.setPages(page.getPages());
        pageResp.setCurrent(page.getCurrent());
        pageResp.setSize(page.getSize());
        return pageResp;
    }

    @Override
    public Result<PageResp<RecommendPosition>> getRecommendPositionList(Integer page, Integer pageSize, Integer type) {
        Page<RecommendPosition> recommendPositionPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<RecommendPosition> queryWrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            queryWrapper.eq(RecommendPosition::getType, type);
        }
        queryWrapper.orderByAsc(RecommendPosition::getPosition);
        Page<RecommendPosition> resultPage = adminRecommendPositionMapper.selectPage(recommendPositionPage, queryWrapper);
        PageResp<RecommendPosition> pageResp = convertPageToResp(resultPage);
        return Result.ok(pageResp);
    }

    @Override
    public Result<RecommendPosition> getRecommendPositionDetail(Long id) {
        RecommendPosition recommendPosition = adminRecommendPositionMapper.selectById(id);
        if (recommendPosition == null) {
            return Result.fail("推荐位不存在");
        }
        return Result.ok(recommendPosition);
    }

    @Override
    public Result<Void> addRecommendPosition(RecommendPositionDTO recommendPositionDTO) {
        // 检查位置是否已被占用
        LambdaQueryWrapper<RecommendPosition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendPosition::getType, recommendPositionDTO.getType())
                .eq(RecommendPosition::getPosition, recommendPositionDTO.getPosition());
        if (adminRecommendPositionMapper.selectCount(queryWrapper) > 0) {
            return Result.fail("该位置已被占用");
        }

        // 检查位置范围（1-30）
        if (recommendPositionDTO.getPosition() < 1 || recommendPositionDTO.getPosition() > 30) {
            return Result.fail("位置必须在1-30之间");
        }

        // 检查类型对应的数量限制（歌单最多5个，歌曲最多30个）
        LambdaQueryWrapper<RecommendPosition> countQueryWrapper = new LambdaQueryWrapper<>();
        countQueryWrapper.eq(RecommendPosition::getType, recommendPositionDTO.getType());
        Long count = adminRecommendPositionMapper.selectCount(countQueryWrapper);
        if (recommendPositionDTO.getType() == 1 && count >= 5) {
            return Result.fail("歌单推荐位最多只能有5个");
        }
        if (recommendPositionDTO.getType() == 2 && count >= 30) {
            return Result.fail("歌曲推荐位最多只能有30个");
        }

        RecommendPosition recommendPosition = new RecommendPosition();
        BeanUtils.copyProperties(recommendPositionDTO, recommendPosition);
        recommendPosition.setCreateTime(LocalDateTime.now());
        recommendPosition.setUpdateTime(LocalDateTime.now());
        adminRecommendPositionMapper.insert(recommendPosition);
        return Result.ok();
    }

    @Override
    public Result<Void> updateRecommendPosition(RecommendPositionDTO recommendPositionDTO) {
        RecommendPosition existingPosition = adminRecommendPositionMapper.selectById(recommendPositionDTO.getId());
        if (existingPosition == null) {
            return Result.fail("推荐位不存在");
        }

        // 检查位置是否已被其他推荐位占用
        LambdaQueryWrapper<RecommendPosition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendPosition::getType, recommendPositionDTO.getType())
                .eq(RecommendPosition::getPosition, recommendPositionDTO.getPosition())
                .ne(RecommendPosition::getId, recommendPositionDTO.getId());
        if (adminRecommendPositionMapper.selectCount(queryWrapper) > 0) {
            return Result.fail("该位置已被占用");
        }

        // 检查位置范围（1-30）
        if (recommendPositionDTO.getPosition() < 1 || recommendPositionDTO.getPosition() > 30) {
            return Result.fail("位置必须在1-30之间");
        }

        BeanUtils.copyProperties(recommendPositionDTO, existingPosition);
        existingPosition.setUpdateTime(LocalDateTime.now());
        adminRecommendPositionMapper.updateById(existingPosition);
        return Result.ok();
    }

    @Override
    public Result<Void> deleteRecommendPosition(Long id) {
        int result = adminRecommendPositionMapper.deleteById(id);
        if (result == 0) {
            return Result.fail("推荐位不存在");
        }
        return Result.ok();
    }

    @Override
    public Result<Void> batchDeleteRecommendPosition(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的推荐位");
        }
        adminRecommendPositionMapper.deleteBatchIds(ids);
        return Result.ok();
    }

    @Override
    public Result<Void> updateRecommendPositionStatus(Long id, Integer status) {
        RecommendPosition recommendPosition = adminRecommendPositionMapper.selectById(id);
        if (recommendPosition == null) {
            return Result.fail("推荐位不存在");
        }
        recommendPosition.setStatus(status);
        recommendPosition.setUpdateTime(LocalDateTime.now());
        adminRecommendPositionMapper.updateById(recommendPosition);
        return Result.ok();
    }

    @Override
    public Result<List<RecommendPosition>> getEnabledRecommendPositions(Integer type) {
        LambdaQueryWrapper<RecommendPosition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecommendPosition::getType, type)
                .eq(RecommendPosition::getStatus, 1)
                .orderByAsc(RecommendPosition::getPosition);
        List<RecommendPosition> recommendPositions = adminRecommendPositionMapper.selectList(queryWrapper);
        return Result.ok(recommendPositions);
    }
}
