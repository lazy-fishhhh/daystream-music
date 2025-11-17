package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.MusicDetailVO;
import com.future.music.mapper.*;
import com.future.music.service.IMinIoService;

import com.future.music.service.IMusicService;
import constants.SystemConstants;
import entity.dto.MusicDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.*;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.UserCollection;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import social.mapper.LikeRecord1Mapper;
import util.AudioDurationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 音乐服务实现类
 * 继承 MyBatis-Plus 的 ServiceImpl 以获得基础 CRUD 能力，实现 IMusicService 接口
 */
@Service
public class MusicServiceImpl extends ServiceImpl<MusicMapper, Music> implements IMusicService {

    // 注入 MinIO 文件存储服务（用于文件上传）
    @Autowired
    private IMinIoService minIoService;

    // 注入音乐音频关联表的 Mapper（操作音乐音频关联数据）
    @Autowired
    private MusicAudioMapper musicAudioMapper;

    // 注入音乐主表的 Mapper（操作音乐核心数据）
    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private LikeRecord1Mapper likeRecordMapper;

    @Autowired
    private UserCollectionMapper userCollectionMapper;

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private MusicianMapper musicianMapper;

    @Autowired
    private MusicTagMapper musicTagMapper;

    @Autowired
    private MusicTagRelationMapper musicTagRelationMapper;

    /**
     * 保存音乐信息（包含音乐主信息、音频、歌词、封面）
     * 标注 @Transactional 确保所有数据库操作在同一事务中，保证数据一致性
     *
     * @param musicDTO 音乐基本信息DTO（包含歌名、歌手等非文件信息）
     * @param lyric    歌词文件
     * @param cover    封面图片文件
     * @param audioMap    音频文件
     * @return 处理结果（成功/失败信息）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap) {
        List<String> uploadedFilePaths = new ArrayList<>(); // 用于回滚删除的文件路径

        try {
            // 1. 基础参数校验
            if (!musicDTO.validate()) {
                return Result.fail("音乐参数有错");
            }

            // 2. 校验歌词和封面文件
            if (!minIoService.isAllowedFile(lyric, "com/future/music") || !minIoService.isAllowedFile(cover, "picture")) {
                return Result.fail("文件格式不正确");
            }
            if (lyric.getSize() > SystemConstants.MAX_LYRIC_SIZE || cover.getSize() > SystemConstants.MAX_PICTURE_SIZE) {
                return Result.fail("文件大小超出限制");
            }

            // 3. 校验音频文件集合（非空+格式+大小）+ 合并上传/时长校验/URL记录
            if (audioMap == null || audioMap.isEmpty()) {
                return Result.fail("至少需上传一个音质的音频文件");
            }

            Map<Integer, String> audioUrlMap = new HashMap<>(); // 音质→音频URL映射
            Map<Integer, Long> audioFileSizeMap = new HashMap<>(); // 音质→文件大小映射（新增，替代后续重复get）
            Long baseDuration = null; // 基准时长（替代audioDurationMap，简化逻辑）

            // 合并循环：一次循环完成 校验→上传→时长校验→URL/大小记录
            for (Map.Entry<Integer, MultipartFile> entry : audioMap.entrySet()) {
                Integer quality = entry.getKey();
                MultipartFile audio = entry.getValue();

                // 3.1 校验音质类型（1-4）
                if (quality == null || !(quality == 1 || quality == 2 || quality == 3 || quality == 4)) {
                    return Result.fail("音质类型不合法：" + quality);
                }

                // 3.2 校验音频文件格式和大小
                if (!minIoService.isAllowedFile(audio, "com/future/music") || audio.getSize() > SystemConstants.MAX_MUSIC_SIZE) {
                    return Result.fail("音频文件（音质：" + quality + "）格式或大小不符合要求");
                }

                // 3.3 上传音频文件
                String audioName = minIoService.uploadFile(audio);
                if (audioName == null) {
                    return Result.fail("音频文件（音质：" + quality + "）上传失败");
                }
                uploadedFilePaths.add(audioName);
                audioUrlMap.put(quality, audioName); // 记录URL
                audioFileSizeMap.put(quality, audio.getSize()); // 记录文件大小（后续直接使用）

                // 3.4 计算并校验音频时长（同一音乐所有音质需一致）
                long duration = AudioDurationUtil.getAudioDuration(audio);
                if (duration == -1) {
                    return Result.fail("音频（音质：" + quality + "）时长计算错误");
                }
                if (baseDuration == null) {
                    baseDuration = duration; // 首次计算作为基准时长
                } else if (!baseDuration.equals(duration)) {
                    return Result.fail("不同音质的音频时长不一致");
                }
            }

            // 4. 上传歌词和封面
            String lyricName = minIoService.uploadFile(lyric);
            if (lyricName == null) return Result.fail("歌词上传失败");
            uploadedFilePaths.add(lyricName);

            String coverName = minIoService.uploadFile(cover);
            if (coverName == null) return Result.fail("封面上传失败");
            uploadedFilePaths.add(coverName);

            // 5. 构建音乐实体（使用基准时长）
            Music music = musicDTO.toEntity();
            music.setCoverUrl(coverName);
            music.setLyricUrl(lyricName);
            music.setDuration(AudioDurationUtil.formatSecondsToMS(baseDuration)); // 直接用基准时长

            // 6. 保存音乐主信息
            if (!this.save(music)) {
                throw new RuntimeException("音乐保存失败");
            }

            // 7. 保存所有音质的音频关联信息（使用提前记录的size，无需重复get(audio)）
            for (Map.Entry<Integer, String> entry : audioUrlMap.entrySet()) {
                Integer quality = entry.getKey();
                String audioUrl = entry.getValue();

                MusicAudio musicAudio = new MusicAudio();
                musicAudio.setAudioUrl(audioUrl);
                musicAudio.setMusicId(music.getId());
                musicAudio.setQualityType(quality);
                musicAudio.setFileSize(audioFileSizeMap.get(quality)); // 直接取记录的大小，避免重复操作

                if (musicAudioMapper.insert(musicAudio) != 1) {
                    throw new RuntimeException("音频（音质：" + quality + "）关联信息保存失败");
                }
            }

            // 8. 关联音乐与标签
            for (Long tagId : musicDTO.getTags()) {
                MusicTagRelation musicTagRelation = new MusicTagRelation();
                musicTagRelation.setMusicId(music.getId());
                musicTagRelation.setTagId(tagId);
                if (musicTagRelationMapper.insert(musicTagRelation) == 0) {
                    throw new RuntimeException("标签关联失败");
                }
            }

        } catch (Exception e) {
            // 回滚：删除已上传的所有文件
            if (!uploadedFilePaths.isEmpty()) {
                minIoService.batchDeleteFiles(uploadedFilePaths);
            }
            return Result.fail("保存音乐失败，请稍后重试：" + e.getMessage());
        }

        return Result.ok();
    }

    /**
     * 删除音乐（包含音乐主信息及关联的音频信息）
     * 标注 @Transactional 确保删除操作的原子性（要么都删，要么都不删）
     *
     * @param musicId 音乐ID
     * @return 处理结果（成功/失败信息）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result removeMusic(Long musicId) {

        try {
            // 校验音乐是否存在（已被删除则直接返回失败）
            if (musicMapper.selectById(musicId) == null) {
                return Result.fail("该音乐已被删除");
            }

            // 1. 删除关联的音频信息（根据音乐ID查询并删除）
            QueryWrapper<MusicAudio> musicAudioQueryWrapper = new QueryWrapper<>();
            musicAudioQueryWrapper.eq("music_id", musicId);  // 条件：music_id = 传入的音乐ID
            // 执行删除并校验结果，若受影响行数为0则抛异常
            if (musicAudioMapper.delete(musicAudioQueryWrapper) == 0) {
                throw new RuntimeException("音乐音频删除失败");
            }

            // 2. 删除关联的标签信息
            QueryWrapper<MusicTagRelation> musicTagRelationQueryWrapper = new QueryWrapper<>();
            musicTagRelationQueryWrapper.eq("music_id", musicId);
            if (musicTagRelationMapper.delete(musicTagRelationQueryWrapper) == 0) {
                throw new RuntimeException("音乐标签关联删除失败");
            }

            //  删除音乐主信息
            if (musicMapper.deleteById(musicId) == 0) {
                throw new RuntimeException("音乐删除失败");
            }
        } catch (Exception e) {
            // 发生异常时手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(e.getMessage());  // 返回失败信息
        }

        // 所有删除操作成功
        return Result.ok();
    }

    /**
     * 根据音乐ID和音质类型，从MinIO下载音频文件并返回给前端，同时更新下载量
     * @param musicId 音乐主键ID（必填）
     * @param quality 音频音质类型（对应MusicAudio表的quality_type字段）
     * @param response HTTP响应对象，用于返回文件流和状态信息
     */
    @Override
    public void downloadMusic(Long musicId, Integer quality, HttpServletResponse response) {
        try {
            // 1. 校验音乐ID有效性
            if (musicId == null) {
                response.setStatus(400); // 参数错误状态码
                response.setHeader("msg", "无效的ID");
                return;
            }

            // 2. 查询音乐主信息，判断是否存在
            Music music = getById(musicId);
            if (music == null) {
                response.setStatus(404); // 资源不存在状态码
                response.setHeader("msg", "音乐不存在");
                return;
            }

            // 3. 根据音乐ID和音质查询对应的音频记录
            QueryWrapper<MusicAudio> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("music_id", musicId) // 关联音乐ID
                    .eq("quality_type", quality); // 匹配音质类型
            MusicAudio musicAudio = musicAudioMapper.selectOne(queryWrapper);
            if (musicAudio == null) {
                response.setStatus(400);
                response.setHeader("msg", "不存在该音质的音频");
                return;
            }

            // 4. 校验MinIO中音频文件是否存在
            if (!minIoService.isObjectExists(musicAudio.getAudioUrl())){
                response.setStatus(404);
                response.setHeader("msg","音频文件不存在或已被删除");
                return; // 文件不存在，直接返回，避免后续操作
            }

            // 5. 设置响应状态和提示信息
            response.setStatus(200); // 成功状态码
            response.setHeader("msg", "下载成功"); // 响应头携带成功信息

            // 6. 从MinIO下载文件并写入响应流（前端触发下载）
            minIoService.downloadFile(musicAudio.getAudioUrl(), response);

            // 7. 更新音乐下载量（自增1）
            music.setDownloadCount(music.getDownloadCount() + 1);
            if (!this.updateById(music)) {
                throw new RuntimeException("更新音乐下载数失败"); // 抛出异常，触发后续异常处理
            }

        } catch (Exception e) {
            // 8. 异常处理：返回服务器错误状态和异常信息
            response.setStatus(500); // 服务器内部错误状态码
            response.setHeader("msg", "下载过程出错：" + e.getMessage());
        }
    }

    /**
     * 音乐基础详情回显（内部复用接口）
     * 查询音乐主信息及关联的音频列表（多音质），封装为MusicDetailVO返回
     * @param musicId 音乐主键ID
     * @return Result<MusicDetailVO> 基础详情VO（含音乐主信息、音频列表）
     */
    @Override
    public Result<MusicDetailVO> echoMusic(Long musicId) {
        // 1. 校验音乐ID有效性（非空且大于0）
        if (musicId == null || musicId <= 0) {
            return Result.fail("音乐ID无效");
        }

        // 2. 查询音乐主信息
        Music music = this.getById(musicId);
        if (music == null) {
            return Result.fail("音乐不存在");
        }

        // 3. 初始化VO并复制音乐主信息
        MusicDetailVO musicDetailVO = new MusicDetailVO();
        musicDetailVO.copyFrom(music); // 调用自定义copyFrom方法，复制核心属性

        // 4. 查询该音乐关联的所有音频记录（多音质）
        QueryWrapper<MusicAudio> wrapper = new QueryWrapper<>();
        wrapper.eq("music_id", musicId); // 按音乐ID关联
        List<MusicAudio> audioList = musicAudioMapper.selectList(wrapper);
        musicDetailVO.setAudioList(audioList); // 给VO设置音频列表（含音质、文件大小等信息）

        // 5. 查询标签
        List<String> tags = musicTagMapper.getTagNamesByMusicId(musicId);
        musicDetailVO.setTags(tags);

        // 返回成功结果（基础详情VO）
        return Result.ok(musicDetailVO);
    }

    /**
     * 音乐信息更新接口（支持文件更新）
     * 支持音乐基础信息、歌词、封面、音频的部分或全量更新，含事务回滚和文件清理逻辑
     * @param musicId 待更新音乐的主键ID
     * @param musicDTO 音乐更新参数DTO（含基础信息、音质等）
     * @param lyric 新上传的歌词文件（可选，为null则不更新）
     * @param cover 新上传的封面文件（可选，为null则不更新）
     * @param audioMap 新上传的音频文件（可选，为null则不更新）
     * @return Result 操作结果（成功/失败提示）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 事务注解：任何异常触发回滚
    public Result updateMusic(Long musicId, MusicDTO musicDTO,
                              MultipartFile lyric, MultipartFile cover, Map<Integer, MultipartFile> audioMap) {
        // 存储新上传的文件路径（用于事务回滚时删除）
        List<String> newUploadedFiles = new ArrayList<>();
        // 存储待删除的旧文件路径（事务成功后删除，避免提前删除导致回滚后文件丢失）
        List<String> oldFilesToDelete = new ArrayList<>();
        // 存储新音频的时长（同一音乐不同音质时长需一致）
        Long newDuration = null;

        try {
            // 1. 校验音乐ID和原始音乐是否存在
            if (musicId == null || musicId <= 0) {
                return Result.fail("音乐ID无效");
            }
            Music oldMusic = this.getById(musicId);
            if (oldMusic == null) {
                return Result.fail("音乐不存在");
            }

            // 2. 校验DTO参数合法性（复用新增时的校验规则）
            if (!musicDTO.validate()) {
                return Result.fail("音乐参数有误");
            }

            // 3. 初始化文件路径（默认沿用旧文件路径）
            String newLyricUrl = oldMusic.getLyricUrl();
            String newCoverUrl = oldMusic.getCoverUrl();

            // 3.1 处理歌词文件更新（仅上传新文件时）
            if (lyric != null && !lyric.isEmpty()) {
                if (!minIoService.isAllowedFile(lyric, "com/future/music") || lyric.getSize() > SystemConstants.MAX_LYRIC_SIZE) {
                    return Result.fail("歌词文件格式或大小不符合要求");
                }
                newLyricUrl = minIoService.uploadFile(lyric);
                if (newLyricUrl == null) {
                    throw new RuntimeException("歌词文件上传失败");
                }
                newUploadedFiles.add(newLyricUrl);
                oldFilesToDelete.add(oldMusic.getLyricUrl());
            }

            // 3.2 处理封面文件更新（仅上传新文件时）
            if (cover != null && !cover.isEmpty()) {
                if (!minIoService.isAllowedFile(cover, "picture") || cover.getSize() > SystemConstants.MAX_PICTURE_SIZE) {
                    return Result.fail("封面文件格式或大小不符合要求");
                }
                newCoverUrl = minIoService.uploadFile(cover);
                if (newCoverUrl == null) {
                    throw new RuntimeException("封面文件上传失败");
                }
                newUploadedFiles.add(newCoverUrl);
                oldFilesToDelete.add(oldMusic.getCoverUrl());
            }

            // 3.3 处理多音质音频更新（audioMap非空时）
            if (audioMap != null && !audioMap.isEmpty()) {
                // 校验每个音频的格式、大小，同时上传并记录信息
                for (Map.Entry<Integer, MultipartFile> entry : audioMap.entrySet()) {
                    Integer quality = entry.getKey();
                    MultipartFile audio = entry.getValue();

                    // 校验音质类型合法性（1-4）
                    if (quality == null || !(quality == 1 || quality == 2 || quality == 3 || quality == 4)) {
                        return Result.fail("音质类型不合法：" + quality);
                    }
                    // 校验音频文件格式和大小
                    if (!minIoService.isAllowedFile(audio, "com/future/music") || audio.getSize() > SystemConstants.MAX_MUSIC_SIZE) {
                        return Result.fail("音频文件（音质：" + quality + "）格式或大小不符合要求");
                    }

                    // 上传新音频文件
                    String newAudioUrl = minIoService.uploadFile(audio);
                    if (newAudioUrl == null) {
                        throw new RuntimeException("音频文件（音质：" + quality + "）上传失败");
                    }
                    newUploadedFiles.add(newAudioUrl);

                    // 计算并校验音频时长（同一音乐所有音质时长需一致）
                    long duration = AudioDurationUtil.getAudioDuration(audio);
                    if (duration == -1) {
                        throw new RuntimeException("音频（音质：" + quality + "）时长计算错误");
                    }
                    if (newDuration == null) {
                        newDuration = duration; // 首次计算时长，作为基准
                    } else if (!newDuration.equals(duration)) {
                        return Result.fail("不同音质的音频时长不一致");
                    }

                    // 查询该音质对应的旧音频记录（用于删除旧文件和更新关联表）
                    QueryWrapper<MusicAudio> audioWrapper = new QueryWrapper<>();
                    audioWrapper.eq("music_id", musicId)
                            .eq("quality_type", quality);
                    MusicAudio oldAudio = musicAudioMapper.selectOne(audioWrapper);

                    // 记录旧音频文件（待删除）
                    if (oldAudio != null) {
                        oldFilesToDelete.add(oldAudio.getAudioUrl());
                    }

                    // 构建并保存音频关联信息（更新或新增）
                    MusicAudio updateAudio = new MusicAudio();
                    updateAudio.setAudioUrl(newAudioUrl);
                    updateAudio.setMusicId(musicId);
                    updateAudio.setQualityType(quality);
                    updateAudio.setFileSize(audio.getSize());

                    if (oldAudio != null) {
                        // 存在旧记录：执行更新
                        updateAudio.setId(oldAudio.getId());
                        if (musicAudioMapper.updateById(updateAudio) != 1) {
                            throw new RuntimeException("音频（音质：" + quality + "）信息更新失败");
                        }
                    } else {
                        // 不存在旧记录：执行新增
                        if (musicAudioMapper.insert(updateAudio) != 1) {
                            throw new RuntimeException("新增音频（音质：" + quality + "）记录失败");
                        }
                    }
                }
            }

            // 4. 构建更新后的音乐实体
            Music updateMusic = musicDTO.toEntity();
            updateMusic.setId(musicId);
            updateMusic.setLyricUrl(newLyricUrl);
            updateMusic.setCoverUrl(newCoverUrl);
            updateMusic.setStatus(0); // 重置为待审核状态
            // 音频更新时同步更新时长，未更新则沿用旧时长
            updateMusic.setDuration(newDuration != null
                    ? AudioDurationUtil.formatSecondsToMS(newDuration)
                    : oldMusic.getDuration());
            // 保留系统维护字段（播放量、下载量不允许修改）
            updateMusic.setPlayCount(oldMusic.getPlayCount());
            updateMusic.setDownloadCount(oldMusic.getDownloadCount());

            // 5. 更新音乐主表信息
            if (!this.updateById(updateMusic)) {
                throw new RuntimeException("音乐主信息更新失败");
            }

            // 6. 事务成功后，删除所有旧文件（歌词、封面、旧音频）
            if (!oldFilesToDelete.isEmpty()) {
                minIoService.batchDeleteFiles(oldFilesToDelete);
            }

            return Result.ok();

        } catch (Exception e) {
            // 7. 异常回滚：删除新上传的所有文件，避免垃圾文件
            if (!newUploadedFiles.isEmpty()) {
                minIoService.batchDeleteFiles(newUploadedFiles);
            }
            return Result.fail("修改音乐失败，请稍后重试：" + e.getMessage());
        }
    }

    /**
     * 音乐完整详情查询接口（面向前端）
     * 在基础详情（echoMusic）之上，补充专辑名称、音乐人名称、标签列表、用户点赞状态
     * @param musicId 音乐主键ID
     * @return Result<MusicDetailVO> 完整详情VO（含关联信息和用户交互状态）
     */
    @Override
    public Result<MusicDetailVO> getOneMusic(Long musicId){
        // 1. 校验音乐ID有效性（非空且大于0）
        if (musicId == null || musicId <= 0) {
            return Result.fail("音乐ID无效");
        }

        //  查询音乐主信息
        Music music = this.getById(musicId);
        if (music == null) {
            return Result.fail("音乐不存在");
        }

        //  初始化VO并复制音乐主信息
        MusicDetailVO musicDetailVO = new MusicDetailVO();
        musicDetailVO.copyFrom(music); // 调用自定义copyFrom方法，复制核心属性

        //  查询标签
        List<String> tags = musicTagMapper.getTagNamesByMusicId(musicId);
        musicDetailVO.setTags(tags);

        // 3.1 补充专辑名称（通过专辑ID关联查询）
        Album album = albumMapper.selectById(musicDetailVO.getAlbumId());
        if (album != null) {
            musicDetailVO.setAlbumName(album.getAlbumName());
        }

        // 3.2 补充音乐人名称（通过音乐人ID关联查询，取艺名）
        Musician musician = musicianMapper.selectById(musicDetailVO.getMusicianId());
        if (musician != null) {
            musicDetailVO.setMusicianName(musician.getStageName());
        }

        // 3.4 判断是否点赞
        QueryWrapper<LikeRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", SecurityUtil.getCurrentUserId()) // 获取当前登录用户ID
                .eq("target_type", 1) // 目标类型：1=音乐（系统定义）
                .eq("target_id", musicId); // 目标ID：音乐ID
        LikeRecord likeRecord = likeRecordMapper.selectOne(wrapper);
        musicDetailVO.setIsLiked(likeRecord != null ? 1 : 0);

        // 3.4 判断是否收藏
        QueryWrapper<UserCollection> collectionWrapper = new QueryWrapper<>();
        collectionWrapper.eq("user_id", SecurityUtil.getCurrentUserId())
                .eq("target_type", 1)
                .eq("target_id", musicId);
        UserCollection userCollection = userCollectionMapper.selectOne(collectionWrapper);
        musicDetailVO.setIsCollected(userCollection != null ? 1 : 0);

        // 4. 返回完整详情结果
        return Result.ok(musicDetailVO);
    }

    //搜索单曲
    @Override
    public PageResp<MusicDetailVO> searchMusic(SearchDTO dto) {
        // 1. 校验关键词
        if (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty()) {
            return new PageResp<>();
        }
        String keyword = "%" + dto.getKeyword().trim() + "%"; // 模糊匹配前缀后缀

        // 2. 构建分页参数
        Page<Music> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 3. 构建多条件查询（音乐名、标签名、音乐人名字 模糊匹配）
        QueryWrapper<Music> queryWrapper = new QueryWrapper<>();
        // 条件1：音乐名模糊匹配
        queryWrapper.like("m.music_name", keyword)
                // 条件2：音乐人名字模糊匹配（关联musician表）
                .or().like("mu.name", keyword)
                // 条件3：标签名模糊匹配（关联music_tag表）
                .or().like("mt.tag_name", keyword)
                // 过滤音乐状态（仅查询上架状态，按需调整）
                .eq("m.status", 1);

        // 4. 执行多表联查+分页+排序（SQL已含ORDER BY play_count DESC）
        IPage<Music> musicPage = musicMapper.searchMusicByMultiCondition(page, queryWrapper);

        // 5. 转换为MusicDetailVO并填充关联信息（专辑名、点赞状态、音频列表等）
        List<MusicDetailVO> voList = new ArrayList<>();
        for (Music music : musicPage.getRecords()) {
            MusicDetailVO vo = new MusicDetailVO();
            BeanUtils.copyProperties(music, vo);

            // 5.1 关联专辑名称（通过albumId查album表）
            Album album = albumMapper.selectById(music.getAlbumId());
            if (album != null) {
                vo.setAlbumName(album.getAlbumName());
            }

            // 5.2 关联点赞、收藏状态
            if (SecurityUtil.getCurrentUserId() != null) {
                QueryWrapper<LikeRecord> likeWrapper = new QueryWrapper<>();
                likeWrapper.eq("target_id", music.getId())
                        .eq("target_type", 1)
                        .eq("user_id", SecurityUtil.getCurrentUserId());
                LikeRecord likeRecord = likeRecordMapper.selectOne(likeWrapper);
                vo.setIsLiked(likeRecord != null ? 1 : 0);

                QueryWrapper<UserCollection> collectionWrapper = new QueryWrapper<>();
                collectionWrapper.eq("user_id", SecurityUtil.getCurrentUserId())
                        .eq("target_type", 1)
                        .eq("target_id", music.getId());
                UserCollection userCollection = userCollectionMapper.selectOne(collectionWrapper);
                vo.setIsCollected(userCollection != null ? 1 : 0);
            } else {
                vo.setIsLiked(0); // 未登录默认未点赞
                vo.setIsCollected(0);
            }

            // 5.3 关联音频列表（通过musicId查music_audio表）
//            QueryWrapper<MusicAudio> audioWrapper = new QueryWrapper<>();
//            audioWrapper.eq("music_id", music.getId());
//            List<MusicAudio> audioList = musicAudioMapper.selectList(audioWrapper);
//            vo.setAudioList(audioList);

            // 5.4 关联音乐人名字（通过musicianId查musician表）
            Musician musician = musicianMapper.selectById(music.getMusicianId());
            if (musician != null) {
                vo.setMusicianName(musician.getStageName());
            }

            voList.add(vo);
        }

        // 6. 封装分页结果
        PageResp<MusicDetailVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(musicPage.getTotal());
        pageResult.setPages((int) musicPage.getPages());

        return pageResult;
    }

}