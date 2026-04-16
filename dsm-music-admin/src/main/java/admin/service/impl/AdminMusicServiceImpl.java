package admin.service.impl;

import admin.domain.dto.MusicAuditDTO;
import admin.domain.dto.MusicListDTO;
import admin.domain.vo.MusicVO;
import admin.service.AdminMusicService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.login.exception.BusinessException;
import com.future.login.util.SecurityUtil;
import admin.mapper.AdminMusicMapper;
import admin.mapper.AdminMusicianMapper;
import admin.mapper.AdminAlbumMapper;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.social.LikeRecord;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private BaseMapper<LikeRecord> likeRecordMapper;

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

        // 补充音频列表
        try {
            List<entity.pojo.music.MusicAudio> audioList = musicMusicMapper.selectAudioByMusicId(musicId);
            if (audioList != null && !audioList.isEmpty()) {
                musicVO.setAudioList(audioList);
            }
        } catch (Exception e) {
            log.error("查询音乐音频信息失败，musicId: {}", musicId, e);
        }

        // 检查是否被当前用户点赞过
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) {
                Integer isLiked = musicMusicMapper.checkMusicLiked(userId, musicId);
                musicVO.setIsLiked(isLiked != null && isLiked > 0 ? 1 : 0);
            } else {
                musicVO.setIsLiked(0);
            }
        } catch (Exception e) {
            log.error("检查音乐点赞状态失败，musicId: {}", musicId, e);
            musicVO.setIsLiked(0);
        }

        // 查询驳回原因
        try {
            String rejectReason = musicMusicMapper.getMusicRejectReason(musicId);
            musicVO.setRejectReason(rejectReason);
        } catch (Exception e) {
            log.error("查询音乐驳回原因失败，musicId: {}", musicId, e);
        }

        return Result.ok(musicVO);
    }

    @Override
    public Result<PageResp<MusicVO>> getMusicAuditList(MusicListDTO musicListDTO) {
        // 创建分页对象
        Page<MusicVO> page = new Page<>(musicListDTO.getPageNum(), musicListDTO.getPageSize());

        // 使用连表查询获取音乐作品审核列表
        IPage<MusicVO> musicVOPage = musicMusicMapper.selectMusicAuditList(page, musicListDTO.getStatus(), musicListDTO.getKeyword());

        // 为每首音乐查询音频列表和点赞状态
        List<MusicVO> records = musicVOPage.getRecords();
        if (CollectionUtils.isNotEmpty(records)) {
            Long userId = null;
            try {
                userId = SecurityUtil.getCurrentUserId();
            } catch (Exception e) {
                log.error("获取当前用户ID失败", e);
            }

            for (MusicVO musicVO : records) {
                // 查询音频列表
                try {
                    List<entity.pojo.music.MusicAudio> audioList = musicMusicMapper.selectAudioByMusicId(musicVO.getId());
                    if (audioList != null && !audioList.isEmpty()) {
                        musicVO.setAudioList(audioList);
                    }
                } catch (Exception e) {
                    log.error("查询音乐音频信息失败，musicId: {}", musicVO.getId(), e);
                }

                // 检查是否被当前用户点赞过
                try {
                    if (userId != null) {
                        LambdaQueryWrapper<LikeRecord> lqw = new LambdaQueryWrapper<>();

                        Integer isLiked = musicMusicMapper.checkMusicLiked(userId, musicVO.getId());
                        musicVO.setIsLiked(isLiked != null && isLiked > 0 ? 1 : 0);
                    } else {
                        musicVO.setIsLiked(0);
                    }
                } catch (Exception e) {
                    log.error("检查音乐点赞状态失败，musicId: {}", musicVO.getId(), e);
                    musicVO.setIsLiked(0);
                }
            }
        }

        // 封装分页结果
        PageResp<MusicVO> pageResp = new PageResp<>();
        pageResp.setRecords(records);
        pageResp.setTotal(musicVOPage.getTotal());
        pageResp.setPages(musicVOPage.getPages());

        return Result.ok(pageResp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> auditMusic(MusicAuditDTO musicAuditDTO) {
        log.info("音乐作品审核请求: {}", musicAuditDTO);
        try {
            if (musicAuditDTO.getId() == null) {
                return Result.fail("音乐作品ID不能为空");
            }
            if (musicAuditDTO.getStatus() == null || (musicAuditDTO.getStatus() != 1 && musicAuditDTO.getStatus() != 2)) {
                return Result.fail("审核状态无效，只能为1(通过)或2(驳回)");
            }
            // 检查音乐作品是否存在且处于待审核状态
            Music music = musicMusicMapper.selectById(musicAuditDTO.getId());
            if (music == null) {
                return Result.fail("音乐作品不存在");
            }
            if (music.getStatus() != 0) {
                return Result.fail("只有待审核的音乐作品才能进行审核操作");
            }
            // 更新音乐作品状态
            music.setStatus(musicAuditDTO.getStatus());
            int updateCount = musicMusicMapper.updateById(music);
            if (updateCount > 0) {
                return Result.ok("音乐作品审核成功");
            } else {
                return Result.fail("音乐作品审核失败");
            }
        } catch (Exception e) {
            log.error("音乐作品审核失败: {}", e.getMessage(), e);
            return Result.fail("音乐作品审核失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchPass(MusicAuditDTO musicAuditDTO) {
        log.info("批量通过音乐作品审核请求: {}", musicAuditDTO);
        try {
            if (musicAuditDTO.getIds() == null || musicAuditDTO.getIds().isEmpty()) {
                return Result.fail("请选择要审核的音乐作品");
            }
            // 批量更新音乐作品状态为通过(1)
            List<Long> musicIds = musicAuditDTO.getIds();
            LambdaQueryWrapper<Music> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Music::getId, musicIds);
            queryWrapper.eq(Music::getStatus, 0); // 只更新待审核的音乐作品
            Music updateMusic = new Music();
            updateMusic.setStatus(1);
            int updateCount = musicMusicMapper.update(updateMusic, queryWrapper);
            if (updateCount > 0) {
                return Result.ok("批量通过音乐作品审核成功");
            } else {
                return Result.fail("批量通过音乐作品审核失败");
            }
        } catch (Exception e) {
            log.error("批量通过音乐作品审核失败: {}", e.getMessage(), e);
            return Result.fail("批量审核通过失败：所选作品均非待审核状态或已被处理");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchRefuse(MusicAuditDTO musicAuditDTO) {
        log.info("批量驳回音乐作品审核请求: {}", musicAuditDTO);
        try {
            if (musicAuditDTO.getIds() == null || musicAuditDTO.getIds().isEmpty()) {
                return Result.fail("请选择要审核的音乐作品");
            }
            // 批量更新音乐作品状态为驳回(2)
            List<Long> musicIds = musicAuditDTO.getIds();
            LambdaQueryWrapper<Music> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Music::getId, musicIds);
            queryWrapper.eq(Music::getStatus, 0); // 只更新待审核的音乐作品
            Music updateMusic = new Music();
            updateMusic.setStatus(2);
            int updateCount = musicMusicMapper.update(updateMusic, queryWrapper);
            if (updateCount > 0) {
                return Result.ok("批量驳回音乐作品审核成功");
            } else {
                return Result.fail("批量驳回音乐作品审核失败");
            }
        } catch (Exception e) {
            log.error("批量驳回音乐作品审核失败: {}", e.getMessage(), e);
            return Result.fail("批量审核驳回失败：所选作品均非待审核状态或已被处理");
        }
    }
}
