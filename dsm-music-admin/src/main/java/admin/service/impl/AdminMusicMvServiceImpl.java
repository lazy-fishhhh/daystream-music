package admin.service.impl;

import admin.domain.dto.MvAuditDTO;
import admin.domain.dto.MvListDTO;
import admin.mapper.AdminMusicMvMapper;
import admin.service.AdminMusicMvService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.exception.BusinessException;
import entity.pojo.music.MusicMv;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.MusicMvVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Arrays;
import java.util.List;

/**
 * MV审核服务实现类
 */
@Service
@Slf4j
public class AdminMusicMvServiceImpl implements AdminMusicMvService {

    @Resource
    private AdminMusicMvMapper adminMusicMvMapper;

    @Override
    public Result<PageResp<MusicMvVO>> getMvAuditList(MvListDTO mvListDTO) {
        log.info("获取MV审核列表请求: {}", mvListDTO);
        try {
            // 创建分页对象
            Page<MusicMvVO> page = new Page<>(mvListDTO.getPageNum(), mvListDTO.getPageSize());
            // 查询MV审核列表
            IPage<MusicMvVO> mvPage = adminMusicMvMapper.selectMvAuditList(page, mvListDTO.getStatus());
            // 封装返回结果
            PageResp<MusicMvVO> pageResp = new PageResp<>();
            pageResp.setTotal(mvPage.getTotal());
            pageResp.setRecords(mvPage.getRecords());
            pageResp.setCurrent(mvListDTO.getPageNum());
            pageResp.setSize(mvListDTO.getPageSize());
            return Result.ok(pageResp);
        } catch (Exception e) {
            log.error("获取MV审核列表失败: {}", e.getMessage(), e);
            return Result.fail("获取MV审核列表失败");
        }
    }

    @Override
    public Result<MusicMvVO> getMvAuditDetail(Long mvId) {
        log.info("获取MV审核详情请求: {}", mvId);
        try {
            if (mvId == null) {
                return Result.fail("MV ID不能为空");
            }
            // 查询MV审核详情
            MusicMvVO musicMvVO = adminMusicMvMapper.selectMvAuditDetail(mvId);
            if (musicMvVO == null) {
                return Result.fail("MV不存在");
            }
            return Result.ok(musicMvVO);
        } catch (Exception e) {
            log.error("获取MV审核详情失败: {}", e.getMessage(), e);
            throw new BusinessException("MV审核失败", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> auditMv(MvAuditDTO mvAuditDTO) {
        log.info("MV审核请求: {}", mvAuditDTO);
        try {
            if (mvAuditDTO.getId() == null) {
                return Result.fail("MV ID不能为空");
            }
            if (mvAuditDTO.getStatus() == null || (mvAuditDTO.getStatus() != 1 && mvAuditDTO.getStatus() != 2)) {
                return Result.fail("审核状态无效，只能为1(通过)或2(驳回)");
            }
            // 检查MV是否存在且处于待审核状态
            MusicMv musicMv = adminMusicMvMapper.selectById(mvAuditDTO.getId());
            if (musicMv == null) {
                return Result.fail("MV不存在");
            }
            if (musicMv.getStatus() != 0) {
                return Result.fail("只有待审核的MV才能进行审核操作");
            }
            // 更新MV状态
            musicMv.setStatus(mvAuditDTO.getStatus());
            int updateCount = adminMusicMvMapper.updateById(musicMv);
            if (updateCount > 0) {
                return Result.ok("MV审核成功");
            } else {
                return Result.fail("MV审核失败");
            }
        } catch (Exception e) {
            log.error("MV审核失败: {}", e.getMessage(), e);
            return Result.fail("MV审核失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchPassMv(MvAuditDTO mvAuditDTO) {
        log.info("批量通过MV审核请求: {}", mvAuditDTO);
        try {
            if (mvAuditDTO.getIds() == null) {
                return Result.fail("请选择要审核的MV");
            }
            // 批量更新MV状态为通过(1)
            List<Long> mvIds = mvAuditDTO.getIds();
            LambdaQueryWrapper<MusicMv> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(MusicMv::getId, mvIds);
            queryWrapper.eq(MusicMv::getStatus, 0); // 只更新待审核的MV
            MusicMv updateMusicMv = new MusicMv();
            updateMusicMv.setStatus(1);
            int updateCount = adminMusicMvMapper.update(updateMusicMv, queryWrapper);
            if (updateCount > 0) {
                return Result.ok("批量通过MV审核成功");
            } else {
                return Result.fail("批量通过MV审核失败");
            }
        } catch (Exception e) {
            log.error("批量通过MV审核失败: {}", e.getMessage(), e);
            return Result.fail("批量审核通过失败：所选作品均非待审核状态或已被处理");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchRefuseMv(MvAuditDTO mvAuditDTO) {
        log.info("批量驳回MV审核请求: {}", mvAuditDTO);
        try {
            if (mvAuditDTO.getIds() == null ) {
                return Result.fail("请选择要审核的MV");
            }
            // 批量更新MV状态为驳回(2)
            List<Long> mvIds = mvAuditDTO.getIds();
            LambdaQueryWrapper<MusicMv> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(MusicMv::getId, mvIds);
            queryWrapper.eq(MusicMv::getStatus, 0); // 只更新待审核的MV
            MusicMv updateMusicMv = new MusicMv();
            updateMusicMv.setStatus(2);
            int updateCount = adminMusicMvMapper.update(updateMusicMv, queryWrapper);
            if (updateCount > 0) {
                return Result.ok("批量驳回MV审核成功");
            } else {
                return Result.fail("批量驳回MV审核失败");
            }
        } catch (Exception e) {
            log.error("批量驳回MV审核失败: {}", e.getMessage(), e);
            return Result.fail("批量审核通过失败：所选作品均非待审核状态或已被处理");
        }
    }
}