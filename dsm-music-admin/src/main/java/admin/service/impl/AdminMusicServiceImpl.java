package admin.service.impl;

import admin.domain.dto.MusicAuditDTO;
import admin.domain.dto.MusicListDTO;
import admin.domain.vo.MusicVO;
import admin.service.AdminMusicService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.util.SecurityUtil;
import admin.mapper.AdminMusicMapper;
import admin.mapper.AdminMusicianMapper;
import admin.mapper.AdminAlbumMapper;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 音乐作品审核服务实现类
 */
@Slf4j
@Service
public class AdminMusicServiceImpl implements AdminMusicService {

    @Resource
    private AdminMusicMapper musicMusicMapper;

    @Resource
    private AdminAlbumMapper adminAlbumMapper;

    @Resource
    private AdminMusicianMapper adminMusicianMapper;

    @Override
    public Result<MusicVO> getMusicAuditDetail(Long musicId) {
        // 参数校验
        if (musicId == null || musicId <= 0) {
            return Result.fail("音乐作品ID参数错误");
        }

        // 查询音乐作品
        Music music = musicMusicMapper.selectById(musicId);
        if (music == null) {
            return Result.fail("音乐作品不存在");
        }

        // 转换为VO对象
        MusicVO musicVO = new MusicVO();
        BeanUtils.copyProperties(music, musicVO);

        // 补充专辑名称
        if (music.getAlbumId() != null) {
            Album album = adminAlbumMapper.selectById(music.getAlbumId());
            if (album != null) {
                musicVO.setAlbumName(album.getAlbumName());
            }
        }

        // 补充音乐人名称
        if (music.getMusicianId() != null) {
            Musician musician = adminMusicianMapper.selectById(music.getMusicianId());
            if (musician != null) {
                musicVO.setMusicianName(musician.getStageName());
            }
        }

        return Result.ok(musicVO);
    }

    @Override
    public Result<PageResp<MusicVO>> getMusicAuditList(MusicListDTO musicListDTO) {
        // 创建分页对象
        Page<MusicVO> page = new Page<>(musicListDTO.getPageNum(), musicListDTO.getPageSize());

        // 使用连表查询获取音乐作品审核列表
        IPage<MusicVO> musicVOPage = musicMusicMapper.selectMusicAuditList(page, musicListDTO.getStatus());

        // 封装分页结果
        PageResp<MusicVO> pageResp = new PageResp<>();
        pageResp.setRecords(musicVOPage.getRecords());
        pageResp.setTotal(musicVOPage.getTotal());
        pageResp.setPages(musicVOPage.getPages());

        return Result.ok(pageResp);
    }

    @Override
    public Result<String> auditMusic(MusicAuditDTO musicAuditDTO) {
        // 参数校验
        if (musicAuditDTO.getId() == null || musicAuditDTO.getStatus() == null) {
            return Result.fail("参数错误");
        }

        // 验证状态值是否合法
        if (musicAuditDTO.getStatus() != 1 && musicAuditDTO.getStatus() != 2) {
            return Result.fail("审核状态值不合法");
        }

        // 验证音乐作品是否存在且为待审核状态
        Music music = musicMusicMapper.selectById(musicAuditDTO.getId());
        if (music == null) {
            return Result.fail("音乐作品不存在");
        }
        if (music.getStatus() != 0) {
            return Result.fail("该音乐作品不是待审核状态");
        }

        // 更新音乐作品状态
        music.setStatus(musicAuditDTO.getStatus());
        musicMusicMapper.updateById(music);

        return Result.ok("审核成功");
    }

    /**
     * 批量审核通过音乐作品
     *
     * @param musicAuditDTO 审核请求DTO
     * @return 审核结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchPass(MusicAuditDTO musicAuditDTO) {
        // 1. 入参非空校验
        if (musicAuditDTO == null) {
            log.warn("批量审核通过接口入参为空");
            return Result.fail("请求参数不能为空");
        }

        // 2. 审核ID列表校验（包含空、空集合、重复ID、非法ID）
        if (CollectionUtils.isEmpty(musicAuditDTO.getIds())) {
            log.warn("批量审核通过接口未选择任何音乐作品ID");
            return Result.fail("请选择要审核的音乐作品");
        }

        // 去重+过滤非法ID（避免无效ID导致的异常）
        var validIds = musicAuditDTO.getIds().stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (CollectionUtils.isEmpty(validIds)) {
            log.warn("批量审核通过接口有效音乐作品ID为空，原始ID列表：{}", musicAuditDTO.getIds());
            return Result.fail("未检测到有效的音乐作品ID，请检查选择的内容");
        }

        try {
            // 3. 构建查询条件（添加字段防注入，明确查询字段）
            QueryWrapper<Music> musicQueryWrapper = new QueryWrapper<>();
            musicQueryWrapper.in("id", validIds)
                    .eq("status", 0) // 仅审核待审核状态的作品
                    .select("id"); // 只查询ID字段，提升性能

            // 4. 构建更新对象
            Music updateMusic = new Music();
            updateMusic.setStatus(1); // 审核通过状态

            // 5. 执行更新操作
            int musicUpdateCount = musicMusicMapper.update(updateMusic, musicQueryWrapper);

            // 6. 结果校验与日志记录
            if (musicUpdateCount > 0) {
                log.info("批量审核通过成功，有效ID数：{}，实际更新数：{}", validIds.size(), musicUpdateCount);
                // 补充更新数提示，更友好
                return Result.ok(String.format("批量审核通过成功，共处理%d个作品，其中%d个审核通过", validIds.size(), musicUpdateCount));
            } else {
                log.warn("批量审核通过失败，有效ID数：{}，无符合条件的待审核作品", validIds.size());
                return Result.fail("批量审核通过失败：所选作品均非待审核状态或已被处理");
            }
        } catch (Exception e) {
            // 7. 异常捕获与日志记录（包含堆栈信息）
            log.error("批量审核通过接口执行异常，有效ID列表：{}", validIds, e);
            // 对外隐藏具体异常信息，返回通用提示
            return Result.fail("系统异常，批量审核操作失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务控制，所有异常都回滚
    public Result<String> batchRefuse(MusicAuditDTO musicAuditDTO) {
        // 1. 基础入参校验 - 防止空指针
        if (musicAuditDTO == null) {
            log.warn("批量驳回接口入参为空，请求被拒绝");
            return Result.fail("请求参数不能为空");
        }

        // 2. 审核ID列表精细化校验
        List<Long> originIds = musicAuditDTO.getIds();
        if (CollectionUtils.isEmpty(originIds)) {
            log.warn("批量驳回接口未选择任何音乐作品ID");
            return Result.fail("请选择要审核的音乐作品");
        }

        // 过滤无效ID（null/非正数）+ 去重，避免无效SQL和重复更新
        List<Long> validIds = originIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validIds)) {
            log.warn("批量驳回接口有效ID为空，原始ID列表：{}", originIds);
            return Result.fail("未检测到有效的音乐作品ID，请检查选择的内容（ID不能为null/负数）");
        }

        // 3. 驳回原因校验
        String refuseReason = musicAuditDTO.getReason();
        if (refuseReason == null || refuseReason.trim().isEmpty()) {
            log.warn("批量驳回接口未填写驳回原因，有效ID列表：{}", validIds);
            return Result.fail("驳回操作必须填写驳回原因");
        }

        try {
            // 4. 构建精准的更新条件 - 仅更新待审核状态的作品
            QueryWrapper<Music> musicQueryWrapper = new QueryWrapper<>();
            musicQueryWrapper.in("id", validIds)
                    .eq("status", 0)
                    .select("id");

            // 5. 构建更新对象 - 完善业务字段
            Music updateMusic = new Music();
            updateMusic.setStatus(2); // 驳回状态（2）
            // 6. 执行更新操作
            int musicUpdateCount = musicMusicMapper.update(updateMusic, musicQueryWrapper);

            // 7. 结果校验与日志记录
            if (musicUpdateCount > 0) {
                log.info("批量驳回成功，筛选后有效ID数：{}，实际更新数：{}，驳回原因：{}",
                        validIds.size(), musicUpdateCount, refuseReason);
                // 友好的返回提示，明确处理结果
                return Result.ok(String.format(
                        "批量驳回成功！共筛选出%d个有效作品，其中%d个待审核作品已驳回",
                        validIds.size(), musicUpdateCount));
            } else {
                log.warn("批量驳回失败，有效ID数：{}，无符合条件的待审核作品", validIds.size());
                return Result.fail("批量驳回失败：所选作品均非待审核状态，或已被其他审核人处理");
            }

        } catch (Exception e) {
            // 8. 全局异常捕获 - 避免接口崩溃，隐藏内部异常
            log.error("批量驳回接口执行异常，有效ID列表：{}，驳回原因：{}", validIds, refuseReason, e);
            // 对外返回通用提示，保护系统信息
            return Result.fail("系统异常，批量驳回操作失败，请稍后重试");
        }
    }
}
