package com.future.music.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.login.exception.BusinessException;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.dto.MusicMvDTO;
import com.future.music.mapper.LikeRecordMapper;
import com.future.music.mapper.MusicMapper;
import com.future.music.mapper.MusicMvMapper;
import com.future.music.mapper.UserCollectionMapper;
import com.future.music.service.IFileService;
import com.future.music.service.IMinIoService;
import com.future.music.service.IMusicMvService;
import com.future.music.service.IMvQualityRelationService;
import entity.pojo.music.Music;
import entity.pojo.music.MusicMv;
import entity.pojo.music.MvQualityRelation;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.UserCollection;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.dto.FileUploadDTO;

import entity.vo.MusicMvVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Objects;

/**
 * MV相关的Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicMvServiceImpl extends ServiceImpl<MusicMvMapper, MusicMv> implements IMusicMvService {


    private final MusicMvMapper musicMvMapper;

    private final MusicMapper musicMapper;
    private final IMvQualityRelationService mvQualityRelationService;


    private final IMinIoService minIoService;

    private final IFileService fileService;

    private final LikeRecordMapper likeRecordMapper;

    private final UserCollectionMapper userCollectionMapper;

    /**
     * 上传MV文件
     * @param fileUploadDTO 文件上传参数
     * @return 上传结果
     */
    @Override
    public Result<String> uploadMv(FileUploadDTO fileUploadDTO) {
        try {
            log.info("开始上传MV文件，md5Value={}, chunk={}, chunks={}, operation={}", 
                fileUploadDTO.getMd5Value(), fileUploadDTO.getChunk(), fileUploadDTO.getChunks(), fileUploadDTO.getOperation());
            
            if (fileUploadDTO.getFile() == null || fileUploadDTO.getFile().isEmpty()) {
                log.error("上传文件为空");
                return Result.fail("上传文件不能为空");
            }
            
            String mvUrl = fileService.uploadLargeFile(fileUploadDTO);
            log.info("MV文件上传成功，mvUrl={}", mvUrl);
            return Result.ok(mvUrl);
        } catch (IllegalArgumentException e) {
            log.error("MV文件上传参数异常: {}", e.getMessage(), e);
            return Result.fail("MV文件上传失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("MV文件上传IO异常: {}", e.getMessage(), e);
            return Result.fail("MV文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("MV文件上传异常: {}", e.getMessage(), e);
            return Result.fail("MV文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 合并MV分片文件
     * @param fileUploadDTO 文件上传参数
     * @return 合并结果
     */
    @Override
    public Result<String> mergeMvChunks(FileUploadDTO fileUploadDTO) {
        try {
            // 从fileUploadDTO中获取必要信息
            String md5Value = fileUploadDTO.getMd5Value();
            String originalFilename = fileUploadDTO.getOriginalFilename();
            
            // 调用文件服务的合并方法
            String result = fileService.merge(md5Value, originalFilename);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("MV分片文件合并失败: " + e.getMessage());
        }
    }

    /**
     * 创建MV
     * @param musicMvDTO MV相关参数
     * @return 创建结果
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createMv(MusicMvDTO musicMvDTO) {
        // ========== 1. 前置参数校验 ==========
        if (musicMvDTO == null) {
            return Result.fail("创建MV失败：请求参数不能为空");
        }
        // 核心字段非空校验
        if (!StringUtils.hasText(musicMvDTO.getMvName())) {
            return Result.fail("创建MV失败：MV名称不能为空");
        }
        if (musicMvDTO.getMusicId() == null || musicMvDTO.getMusicId() <= 0) {
            return Result.fail("创建MV失败：关联音乐ID必须为正整数");
        }
        if (musicMvDTO.getDuration() == null || musicMvDTO.getDuration() <= 0) {
            return Result.fail("创建MV失败：MV时长必须为正整数（秒）");
        }
        // 状态合法性校验
        if (musicMvDTO.getStatus() == null || !List.of(0, 1, 2).contains(musicMvDTO.getStatus())) {
            return Result.fail("创建MV失败：状态值只能是0（待审核）、1（上架）、2（下架）");
        }
        // VIP标识合法性校验
        if (musicMvDTO.getIsVip() == null || !List.of(0, 1).contains(musicMvDTO.getIsVip())) {
            return Result.fail("创建MV失败：VIP标识只能是0（否）、1（是）");
        }

        // ========== 2. 业务唯一性校验 ==========
        LambdaQueryWrapper<Music> queryWrapper = new LambdaQueryWrapper<>() ;
            queryWrapper.eq(Music::getId, musicMvDTO.getMusicId());
            Music music = musicMapper.selectOne(queryWrapper);

            if (music == null) {
                return Result.fail("创建MV失败：关联音乐不存在");
            }


        LambdaQueryWrapper<MusicMv> lqw = Wrappers.lambdaQuery(MusicMv.class)
                .eq(MusicMv::getMusicId, musicMvDTO.getMusicId());
        MusicMv existingMv = musicMvMapper.selectOne(lqw);
        if (existingMv != null) {
            return Result.fail("该音乐已关联MV，无法重复创建");
        }

        try {
            // ========== 3. 构建MV主表数据 ==========
            MusicMv musicMv = new MusicMv();
            BeanUtils.copyProperties(musicMvDTO, musicMv);

            // 手动赋值补充（覆盖默认值/避免BeanUtils拷贝遗漏）
            musicMv.setPlayCount(0L);
            musicMv.setLikeCount(0L);
            musicMv.setCommentCount(0L);
            musicMv.setShareCount(0L);
            // release_time/create_time/update_time数据库默认当前时间，无需手动赋值
            musicMv.setCreateTime(LocalDateTime.now());
            musicMv.setUpdateTime(LocalDateTime.now());

            // ========== 4. 插入MV主表 ==========
            int insertResult = musicMvMapper.insert(musicMv);
            if (insertResult <= 0) {
                throw new BusinessException("MV异常10002","MV主表数据插入失败");
            }
            Long mvId = musicMv.getId(); // 自增ID赋值成功
            if (mvId == null || mvId <= 0) {
                throw new BusinessException("MV异常10002","MV主键ID生成失败");
            }

            // ========== 5. 处理多画质数据 ==========
            List<MusicMvDTO.MvQualityDTO> qualityList = musicMvDTO.getQualityList();
            if (CollectionUtils.isNotEmpty(qualityList)) {
                List<MvQualityRelation> mvQualityRelations = new ArrayList<>(qualityList.size());
                for (MusicMvDTO.MvQualityDTO qualityDTO : qualityList) {
                    // 单条画质数据校验
                    if (qualityDTO == null) {
                        continue; // 跳过空数据，或抛出异常
                    }
                    if (qualityDTO.getQualityType() == null || !List.of(1, 2, 3, 4).contains(qualityDTO.getQualityType())) {
                        throw new BusinessException("画质类型非法10007","画质类型非法：仅支持1(480P)/2(720P)/3(1080P)/4(4K)");
                    }
                    if (!StringUtils.hasText(qualityDTO.getMvUrl())) {
                        throw new BusinessException("画质类型10008" ,"画质类型" + qualityDTO.getQualityType() + "的播放地址不能为空");
                    }
                    if (qualityDTO.getFileSize() == null || qualityDTO.getFileSize() <= 0) {
                        throw new BusinessException("画质类型10009" ,"画质类型" + qualityDTO.getQualityType() + "的文件大小必须为正整数");
                    }
                    if (qualityDTO.getIsVipLimit() == null || !List.of(0, 1).contains(qualityDTO.getIsVipLimit())) {
                        throw new BusinessException("画质类型10010" ,"画质类型" + qualityDTO.getQualityType() + "的VIP限制标识非法：仅支持0/1");
                    }

                    // 构建画质关联表数据
                    MvQualityRelation qualityRelation = new MvQualityRelation();
                    qualityRelation.setMvId(mvId); // 关联已生成的MV主键
                    qualityRelation.setQualityType(qualityDTO.getQualityType());
                    qualityRelation.setMvUrl(qualityDTO.getMvUrl());
                    qualityRelation.setFileSize(qualityDTO.getFileSize());
                    qualityRelation.setIsVipLimit(qualityDTO.getIsVipLimit());
                    mvQualityRelations.add(qualityRelation);
                }

                // 批量插入画质数据
                if (!mvQualityRelations.isEmpty()) {
                    boolean saveBatchResult = mvQualityRelationService.saveBatch(mvQualityRelations, 100); // 分批插入，每批100条
                    if (!saveBatchResult) {
                        throw new BusinessException("MV异常10003","MV多画质数据批量插入失败");
                    }
                }
            }

            // ========== 6. 返回成功结果 ==========
            return Result.ok(mvId);

        } catch (IllegalArgumentException e) {
            // 参数非法异常（友好提示）
            throw new BusinessException( "MV异常10004","创建MV失败：参数校验不通过 - " + e.getMessage());
        } catch (RuntimeException e) {
            // 业务逻辑异常
            throw new BusinessException( "MV异常10005","创建MV失败：业务处理异常 - " + e.getMessage());
        } catch (Exception e) {
            // 兜底异常
            log.error("MV创建流程异常");
            throw new BusinessException( "MV异常10006","创建MV失败：系统异常，请联系管理员");
        }
    }

    /**
     * 更新MV信息
     *
     * @param mvId        MV ID（非空）
     * @param musicMvDTO  MV相关参数（非空，核心字段校验）
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public Result<Boolean> updateMv(Long mvId, MusicMvDTO musicMvDTO) {
        // ========== 1. 前置参数强校验（快速失败） ==========
        try {

            if (mvId <= 0) {
                log.warn("更新MV失败：MV ID非法，mvId={}", mvId);
                throw new BusinessException("MV异常10001","MV ID非法：MV ID必须为正整数");
            }
            if(musicMvDTO.getReleaseTime() != null){
                return Result.fail("发布时间不允许更改");
            }
            if(musicMvDTO.getIsVip() != null){
                return Result.fail("是否VIP不允许更改");
            }



            // 基础非空校验
            Assert.notNull(mvId, "MV ID不能为空");
            Assert.notNull(musicMvDTO, "MV更新参数不能为空");


            if (musicMvDTO.getDuration() != null && musicMvDTO.getDuration() <= 0) {
                log.warn("更新MV失败：时长非法，mvId={}, duration={}", mvId, musicMvDTO.getDuration());
                    throw new BusinessException("MV异常10001","时长非法：时长必须为正整数");
            }

            // ========== 2. 校验MV存在性（加锁防并发删除） ==========
            // 使用lambda查询+悲观锁
            MusicMv existingMv = this.getOne(
                    Wrappers.lambdaQuery(MusicMv.class)
                            .eq(MusicMv::getId, mvId)
                            .last("FOR UPDATE") // 行级锁，防止更新过程中被删除
            );
            if (existingMv == null) {
                log.error("更新MV失败：MV不存在，mvId={}", mvId);
                throw new BusinessException("MV异常10002","MV不存在，更新失败");
            }

            // ========== 3. 构建更新对象  ==========
            MusicMv updateMv = new MusicMv();
            updateMv.setId(mvId);
            updateMv.setUpdateTime(LocalDateTime.now());

            // 仅拷贝非空字段（避免把null值覆盖到数据库）
            BeanUtil.copyProperties(musicMvDTO, updateMv,
                    // 忽略空值字段 + 固定不更新的字段
                    "id", "createTime", "playCount", "likeCount"); // 根据业务调整忽略字段

            // ========== 4. 执行MV主表更新 ==========
            boolean updateResult = this.updateById(updateMv);
            if (!updateResult) {
                log.error("更新MV主表失败，mvId={}，更新参数={}", mvId, musicMvDTO);
                throw new BusinessException( "MV异常10002","MV主信息更新失败"); // 主动抛异常触发事务回滚
            }
            log.info("MV主信息更新成功，mvId={}", mvId);

            // ========== 5. 处理多画质信息（空值防护 + 数据校验） ==========
            List<MusicMvDTO.MvQualityDTO> qualityList = musicMvDTO.getQualityList();
            if (CollectionUtil.isNotEmpty(qualityList)) {
                // 5.1 删除旧画质信息（先删后加，保证数据一致性）
                boolean removeOld = mvQualityRelationService.remove(
                        Wrappers.lambdaQuery(MvQualityRelation.class)
                                .eq(MvQualityRelation::getMvId, mvId)
                );
                log.info("删除MV旧画质信息，mvId={}，删除结果={}", mvId, removeOld);

                // 5.2 校验并构建新画质列表
                List<MvQualityRelation> mvQualityRelations = new ArrayList<>();
                for (MusicMvDTO.MvQualityDTO qualityDTO : qualityList) {
                    // 单条画质信息校验
                    Assert.notNull(qualityDTO.getQualityType(), "画质类型不能为空，mvId=" + mvId);
                    Assert.notBlank(qualityDTO.getMvUrl(), "画质类型[" + qualityDTO.getQualityType() + "]的播放地址不能为空，mvId=" + mvId);
                    Assert.notNull(qualityDTO.getFileSize(), "文件大小不能为空，mvId=" + mvId + "，画质类型=" + qualityDTO.getQualityType());
                    if (qualityDTO.getFileSize() < 0) {
                        throw new BusinessException( "MV异常10003","文件大小不能为负数，mvId=" + mvId + "，画质类型=" + qualityDTO.getQualityType());
                    }

                    // 构建实体
                    MvQualityRelation qualityRelation = new MvQualityRelation();
                    qualityRelation.setMvId(mvId);
                    qualityRelation.setQualityType(qualityDTO.getQualityType());
                    qualityRelation.setMvUrl(qualityDTO.getMvUrl().trim()); // 去除首尾空格
                    qualityRelation.setFileSize(qualityDTO.getFileSize());
                    qualityRelation.setIsVipLimit(Objects.requireNonNullElse(qualityDTO.getIsVipLimit(), 0)); // 空值默认0
                    mvQualityRelations.add(qualityRelation);
                }

                // 5.3 批量保存新画质信息
                boolean saveQuality = mvQualityRelationService.saveBatch(mvQualityRelations, 100); // 分批插入，每批100条
                if (!saveQuality) {
                    log.error("保存MV画质信息失败，mvId={}，画质列表={}", mvId, qualityList);
                    throw new BusinessException( "MV异常10004", "MV画质信息更新失败");
                }
                log.info("MV画质信息更新成功，mvId={}，新增画质数={}", mvId, qualityList.size());
            } else {
                log.info("MV更新无画质信息变更，mvId={}", mvId);
            }

            // ========== 6. 返回成功结果 ==========
            return Result.ok(true);

        } catch (IllegalArgumentException | BusinessException e) {
            // 业务异常：参数错误、数据不存在等，友好提示
            log.warn("更新MV业务异常，mvId={}，原因：{}", mvId, e.getMessage());
            return Result.fail("更新MV失败：" + e.getMessage());
        } catch (Exception e) {
            // 系统异常：数据库错误、网络异常等，统一兜底
            log.error("更新MV系统异常，mvId={}", mvId, e);
            return Result.fail("更新MV失败：系统异常，请稍后重试");
        }
    }

    /**
     * 删除MV
     * @param mvId MV ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteMv(Long mvId) {
        try {
            // 1. 检查MV是否存在
            MusicMv existingMv = this.getById(mvId);
            if (existingMv == null) {
                return Result.fail("MV不存在");
            }

            // 删除MV多画质信息
            QueryWrapper<MvQualityRelation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("mv_id", mvId);
            mvQualityRelationService.remove(queryWrapper);

            // 3. 删除MV
            boolean deleteResult = this.removeById(mvId);

            return Result.ok(deleteResult);
        } catch (Exception e) {
            return Result.fail("删除MV失败: " + e.getMessage());
        }
    }

    /**
     * 根据MV ID查询MV详情
     *
     * @param mvId MV ID
     * @return MV详情
     */
    @Override
    public Result<MusicMvVO> getMvDetail(Long mvId) {
        try {
            MusicMvVO mvDetail = musicMvMapper.selectMvDetailById(mvId);
            if (mvDetail == null) {
                return Result.fail("MV不存在");
            }
            setLikeAndCollectStatus(mvDetail);

            LambdaUpdateWrapper<MusicMv> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MusicMv::getId, mvId)
                    .setSql("play_count = play_count + 1");
            this.update(null, updateWrapper);

            return Result.ok(mvDetail);
        } catch (Exception e) {
            return Result.fail("查询MV详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据音乐人ID分页查询其所有MV作品
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param musicianId 音乐人ID
     * @return 分页的MV列表
     */
    @Override
    public Result<IPage<MusicMvVO>> getMvsByMusicianId(Long musicianId, Integer pageNum, Integer pageSize) {
        try {
            Page<MusicMvVO> page = new Page<>(pageNum, pageSize);
            IPage<MusicMvVO> mvPage = musicMvMapper.selectMvVOsByMusicianIdPage(page, musicianId);
            if (mvPage != null && !mvPage.getRecords().isEmpty()) {
                mvPage.getRecords().forEach(this::setLikeAndCollectStatus);
            }
            return Result.ok(mvPage);
        } catch (Exception e) {
            return Result.fail("查询音乐人MV列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据音乐ID查询对应的MV
     * @param musicId 音乐ID
     * @return MV详情
     */
    @Override
    public Result<MusicMvVO> getMvByMusicId(Long musicId) {
        try {
            MusicMvVO mvDetail = musicMvMapper.selectMvByMusicId(musicId);
            if (mvDetail == null) {
                return Result.fail("该音乐没有对应的MV");
            }
            setLikeAndCollectStatus(mvDetail);
            return Result.ok(mvDetail);
        } catch (Exception e) {
            return Result.fail("查询音乐对应的MV失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询热门MV
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页的热门MV列表
     */
    @Override
    public Result<PageResp<MusicMvVO>> getHotMvs(Integer pageNum, Integer pageSize) {
        try {
            Page<MusicMvVO> page = new Page<>(pageNum, pageSize);
            IPage<MusicMvVO> mvPage = musicMvMapper.selectHotMvsPage(page);

            PageResp<MusicMvVO> musicMvVOPageResp = convertToPageResp(mvPage);
            if (musicMvVOPageResp != null && !musicMvVOPageResp.getRecords().isEmpty()) {
                musicMvVOPageResp.getRecords().forEach(this::setLikeAndCollectStatus);
            }
            return Result.ok(musicMvVOPageResp);
        } catch (Exception e) {
            return Result.fail("查询热门MV列表失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询最新MV
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页的最新MV列表
     */
    @Override
    public Result<PageResp<MusicMvVO>> getNewMvs(Integer pageNum, Integer pageSize) {
        try {
            Page<MusicMvVO> page = new Page<>(pageNum, pageSize);
            IPage<MusicMvVO> mvPage = musicMvMapper.selectNewMvsPage(page);
            PageResp<MusicMvVO> musicMvVOPageResp = convertToPageResp(mvPage);
            if (musicMvVOPageResp != null && !musicMvVOPageResp.getRecords().isEmpty()) {
                musicMvVOPageResp.getRecords().forEach(this::setLikeAndCollectStatus);
            }
            return Result.ok(musicMvVOPageResp);
        } catch (Exception e) {
            return Result.fail("查询最新MV列表失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询VIP专属MV
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页的VIP专属MV列表
     */
    @Override
    public Result<PageResp<MusicMvVO>> getVipMvs(Integer pageNum, Integer pageSize) {
        try {
            Page<MusicMvVO> page = new Page<>(pageNum, pageSize);
            IPage<MusicMvVO> mvPage = musicMvMapper.selectVipMvsPage(page);
            PageResp<MusicMvVO> musicMvVOPageResp = convertToPageResp(mvPage);
            if (musicMvVOPageResp != null && !musicMvVOPageResp.getRecords().isEmpty()) {
                musicMvVOPageResp.getRecords().forEach(this::setLikeAndCollectStatus);
            }
            return Result.ok(musicMvVOPageResp);
        } catch (Exception e) {
            return Result.fail("查询VIP专属MV列表失败: " + e.getMessage());
        }
    }



    /**
     * 设置MV的点赞和收藏状态
     */
    private void setLikeAndCollectStatus(MusicMvVO musicMvVO) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId != null) {
                // 查询点赞状态
                LikeRecord likeRecord = likeRecordMapper.selectOne(
                        Wrappers.<LikeRecord>lambdaQuery()
                                .eq(LikeRecord::getUserId, userId)
                                .eq(LikeRecord::getTargetType, 3) // 3表示MV
                                .eq(LikeRecord::getTargetId, musicMvVO.getId())
                                .eq(LikeRecord::getIsCanceled, 0)
                );
                musicMvVO.setIsLiked(likeRecord != null ? 1 : 0);

                // 查询收藏状态
                UserCollection userCollection = userCollectionMapper.selectOne(
                        Wrappers.<UserCollection>lambdaQuery()
                                .eq(UserCollection::getUserId, userId)
                                .eq(UserCollection::getTargetType, 3) // 3表示MV
                                .eq(UserCollection::getTargetId, musicMvVO.getId())
                                .eq(UserCollection::getIsCanceled, 0)
                );
                musicMvVO.setIsCollected(userCollection != null ? 1 : 0);
            } else {
                // 用户未登录，默认未点赞和未收藏
                musicMvVO.setIsLiked(0);
                musicMvVO.setIsCollected(0);
            }
        } catch (Exception e) {
            // 如果获取用户信息失败，默认未点赞和未收藏
            musicMvVO.setIsLiked(0);
            musicMvVO.setIsCollected(0);
        }
    }

    public PageResp<MusicMvVO>  convertToPageResp(IPage<MusicMvVO> page) {
        PageResp<MusicMvVO> musicMvVOPageResp = new PageResp<>();
        musicMvVOPageResp.setCurrent(page.getCurrent());
        musicMvVOPageResp.setPages(page.getPages());
        musicMvVOPageResp.setSize(page.getSize());
        musicMvVOPageResp.setTotal(page.getTotal());
        musicMvVOPageResp.setRecords(page.getRecords());
        return musicMvVOPageResp;
    }
}
