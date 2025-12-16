package com.future.music.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.dto.LyricLineDTO;
import com.future.music.domain.dto.MusicPlayDTO;
import com.future.music.domain.dto.MusicPlayReportDTO;
import com.future.music.domain.vo.*;
import com.future.music.entity.message.MusicDownloadCountMsg;
import com.future.music.entity.message.MusicPlayReportMqMessage;
import com.future.music.mapper.*;
import com.future.music.service.IMinIoService;

import com.future.music.service.IMusicService;
import com.future.music.util.AudioMetadataParser;
import com.future.music.util.LyricParseUtil;
import com.future.player.entity.vo.AlbumVO;
import com.future.player.mapper.AlbumMapper;
import com.future.trade.entity.Vip;
import com.future.trade.mapper.VipMapper;
import constants.*;
import entity.dto.MusicDTO;
import entity.dto.SearchDTO;
import entity.pojo.music.*;
import entity.pojo.social.Comment;
import entity.pojo.social.LikeRecord;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import social.mapper.CommentMapper;
import util.AudioDurationUtil;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private LikeRecordMapper likeRecordMapper;

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

    @Autowired
    private UserListenHistoryMapper userListenHistoryMapper;

    @Autowired
    private PlaylistMusicMapper playlistMusicMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Resource
    private VipMapper vipMapper;

    @Resource
    private MusicLyricMapper musicLyricMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient; // 注入 Redisson 客户端

    @Autowired
    private AudioMetadataParser metadataParser;

    @Resource
    private RabbitTemplate rabbitTemplate; // 注入 RabbitMQ 模板


    /**
     * 保存音乐信息（包含音乐主信息、音频、歌词、封面）
     * 标注 @Transactional 确保所有数据库操作在同一事务中，保证数据一致性
     *
     * @param musicDTO 音乐基本信息DTO（包含歌名、歌手等非文件信息）
     * @param lyric    歌词文件
     * @param cover    封面图片文件
     * @param standard    标准音质音频文件
     * @param highDefinition    高音质音频文件
     * @param lossless    无损音质音频文件
     * @param spatialAudio    空间音频文件
     * @return 处理结果（成功/失败信息）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover,
                                  MultipartFile standard,
                                  MultipartFile highDefinition,
                                  MultipartFile lossless,
                                  MultipartFile spatialAudio
                            ) {
        if (SecurityUtil.getCurrentUserId() ==  null){
            return Result.fail("请先登录再进行该操作");
        }
        Map<Integer, MultipartFile> audioMap = new HashMap<>();
        if (standard != null && !standard.isEmpty()){
            audioMap.put(1, standard);
        }
        if (highDefinition != null && !highDefinition.isEmpty()){
            audioMap.put(2, highDefinition);
        }
        if (lossless != null && !lossless.isEmpty()){
            audioMap.put(3, lossless);
        }
        if (spatialAudio != null && !spatialAudio.isEmpty()){
            audioMap.put(4, spatialAudio);
        }

        List<String> uploadedFilePaths = new ArrayList<>(); // 用于回滚删除的文件路径

        try {
            // 1. 基础参数校验
            if (!musicDTO.saveValidate()) {
                throw new RuntimeException("音乐参数有错");
            }

            // 2. 校验歌词和封面文件
            if (!minIoService.isAllowedFile(cover, "picture")) {
                throw new RuntimeException("文件格式不正确");
            }
            if (cover.getSize() > SystemConstants.MAX_PICTURE_SIZE) {
                throw new RuntimeException("文件大小超出限制");
            }

            String lyricUrl = null;
            String lyricContent = null;
            String lyricType = null;
            if (lyric != null){
                if (!minIoService.isAllowedFile(lyric, "lyric")) {
                    throw new RuntimeException("文件格式不正确");
                }
                if (lyric.getSize() > SystemConstants.MAX_LYRIC_SIZE) {
                    throw new RuntimeException("文件大小超出限制");
                }
                // 2.2 识别歌词格式（从文件名后缀提取）
                lyricType = getLyricType(lyric.getOriginalFilename());
                // 2.3 上传歌词文件到MinIO，获取地址
                lyricUrl = minIoService.uploadFile(lyric);
                if (lyricUrl == null) {
                    throw new RuntimeException("歌词上传失败");
                }
                uploadedFilePaths.add(lyricUrl);
                // 2.4 读取歌词原始内容（存入数据库，避免后续解析需下载文件）
                lyricContent = new String(lyric.getBytes(), StandardCharsets.UTF_8);
                // 清理歌词内容中的特殊字符（可选，避免解析异常）
                lyricContent = cleanLyricContent(lyricContent);
            }


            // 3. 校验音频文件集合（非空+格式+大小）+ 合并上传/时长校验/URL记录
            if (audioMap.isEmpty()) {
                throw new RuntimeException("至少需上传一个音质的音频文件");
            }

            Map<Integer, String> audioUrlMap = new HashMap<>(); // 音质→音频URL映射
            Map<Integer, Long> audioFileSizeMap = new HashMap<>(); // 音质→文件大小映射（新增，替代后续重复get）
            Integer baseDuration = null; // 基准时长

            // 合并循环：一次循环完成 校验→上传→时长校验→URL/大小记录
            for (Map.Entry<Integer, MultipartFile> entry : audioMap.entrySet()) {
                Integer quality = entry.getKey();
                MultipartFile audio = entry.getValue();

                // 3.1 校验音质类型（1-4）
                if (quality == null || !(quality == 1 || quality == 2 || quality == 3 || quality == 4)) {
                    throw new RuntimeException("音质类型不合法：" + quality);
                }

                // 3.2 校验音频文件格式和大小
                if (!minIoService.isAllowedFile(audio, "music") || audio.getSize() > SystemConstants.MAX_MUSIC_SIZE) {
                    throw new RuntimeException("音频文件（音质：" + quality + "）格式或大小不符合要求");
                }

                // 3.3 上传音频文件
                String audioName = uploadAudio(audio,quality);
                if (audioName == null) {
                    throw new RuntimeException("音频文件（音质：" + quality + "）上传失败");
                }
                uploadedFilePaths.add(audioName);
                audioUrlMap.put(quality, audioName); // 记录URL
                audioFileSizeMap.put(quality, audio.getSize()); // 记录文件大小（后续直接使用）

                // 3.4 计算并校验音频时长（同一音乐所有音质需一致）
                int duration = AudioDurationUtil.getAudioDuration(audio);
                if (duration == -1) {
                    throw new RuntimeException("音频（音质：" + quality + "）时长计算错误");
                }
                if (baseDuration == null) {
                    baseDuration = duration; // 首次计算作为基准时长
                } else if (!baseDuration.equals(duration)) {
                    throw new RuntimeException("不同音质的音频时长不一致");
                }
            }

            String coverName = minIoService.uploadFile(cover);
            if (coverName == null) {
                throw new RuntimeException("封面上传失败");
            }
            uploadedFilePaths.add(coverName);

            // 5. 构建音乐实体（使用基准时长）
            Music music = musicDTO.toEntity();
            music.setCoverUrl(coverName);
            music.setDuration(baseDuration); // 直接用基准时长

            //音乐人id
            QueryWrapper<Musician>  queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", SecurityUtil.getCurrentUserId());
            Musician musician = musicianMapper.selectOne(queryWrapper);
            if (musician == null) {
                throw new RuntimeException("当前用户未注册为音乐人");
            }
            music.setMusicianId(musician.getId());

            // 6. 保存音乐主信息
            if (!this.save(music)) {
                throw new RuntimeException("音乐保存失败");
            }

            // 保存歌词信息
            if (lyric != null){
                MusicLyric musicLyric = new MusicLyric();
                musicLyric.setMusicId(music.getId());
                musicLyric.setLyricUrl(lyricUrl);
                musicLyric.setLyricContent(lyricContent);
                musicLyric.setLyricType(lyricType);
                if (musicLyricMapper.insert(musicLyric) != 1) {
                    throw new RuntimeException("歌词信息保存失败");
                }
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
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail("保存音乐失败，请稍后重试：" + e.getMessage());
        }

        return Result.ok();
    }

    // 辅助方法1：识别歌词格式（从文件名后缀）
    private String getLyricType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt"; // 默认txt
        }
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (suffix) {
            case "lrc" -> "lrc";
            case "krc" -> "krc";
            default -> "txt";
        };
    }

    // 辅助方法2：清理歌词内容（可选，解决编码/特殊字符问题）
    private String cleanLyricContent(String content) {
        if (content == null) return "";

        // 步骤1：统一换行符（Windows \r\n → Unix \n）
        String cleaned = MusicConstants.CRLF_PATTERN.matcher(content).replaceAll("\n");
        // 步骤2：删除不可见控制字符（保留\n和\r，避免换行丢失）
        cleaned = MusicConstants.CONTROL_CHAR_PATTERN.matcher(cleaned).replaceAll("");
        // 步骤3：清理行首/行尾的多余空白（比如换行前的空格、行首制表符）
        cleaned = MusicConstants.LINE_EDGE_SPACE_PATTERN.matcher(cleaned).replaceAll("");
        // 步骤4：合并行内的连续空白（空格、全角空格、制表符等 → 单个半角空格）
        cleaned = MusicConstants.LINE_BLANK_PATTERN.matcher(cleaned).replaceAll(" ");
        // 步骤5：清理连续空行（可选，避免多行空白）
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n"); // 3个及以上换行 → 2个换行（保留空行但不冗余）

        // 最终去除首尾整体空白（可选，避免歌词开头/结尾有多余换行/空格）
        return cleaned.trim();
    }

    /**
     * 音频检验上传（适配项目的4种音质类型）
     * @param audioFile 音频文件
     * @param qualityTypeCode 音质类型编码（1-4）
     */
    public String uploadAudio(MultipartFile audioFile, int qualityTypeCode) {
        // 1. 基础校验
        if (audioFile.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        // 2. 获取目标音质规格
        ProjectAudioQualitySpec.Spec targetSpec = ProjectAudioQualitySpec.getSpecByCode(qualityTypeCode);

        // 3. 解析音频实际元数据
        AudioMetadataParser.AudioMetadata metadata;
        try {
            metadata = metadataParser.parse(audioFile);
        } catch (Exception e) {
            throw new IllegalArgumentException("音频文件损坏或格式不支持");
        }
        System.out.println(metadata);

        // 4. 校验实际音质是否符合目标规格
        validateAudioQuality(metadata, targetSpec);

        // 5. 校验通过，执行上传
        return minIoService.uploadFile(audioFile);
    }

    /**
     * 适配4种音质的校验逻辑
     */
    private void validateAudioQuality(AudioMetadataParser.AudioMetadata metadata, ProjectAudioQualitySpec.Spec targetSpec) {
        List<String> errors = new ArrayList<>();

        // 1. 采样率校验（Hi-Res需≥96kHz，其余固定44.1kHz）
        if (metadata.getSampleRate() < targetSpec.getMinSampleRate()) {
            errors.add(String.format("采样率%dHz不符合%s要求（需≥%dHz）",
                    metadata.getSampleRate(), targetSpec.getTypeName(), targetSpec.getMinSampleRate()));
        }

        // 2. 位深校验
        if (metadata.getBitDepth() < targetSpec.getMinBitDepth()) {
            errors.add(String.format("位深%dbit不符合%s要求（需≥%dbit）",
                    metadata.getBitDepth(), targetSpec.getTypeName(), targetSpec.getMinBitDepth()));
        }

        // 4. 码率校验（固定码率/最低码率）
        if (metadata.getBitRate() < targetSpec.getMinBitRate()) {
            errors.add(String.format("码率%dbps不符合%s要求（需≥%dbps）",
                    metadata.getBitRate(), targetSpec.getTypeName(), targetSpec.getMinBitRate()));
        }
        // 标准音质需严格128kbps，高清需严格320kbps
        if (targetSpec.getTypeCode() == 1 && metadata.getBitRate() != 128) {
            errors.add("标准音质需严格为128kbps，当前为" + metadata.getBitRate() + "kbps");
        }
        if (targetSpec.getTypeCode() == 2 && metadata.getBitRate() != 320) {
            errors.add("高清音质需严格为320kbps，当前为" + metadata.getBitRate() + "kbps");
        }

        // 5. 声道数校验（全为双声道）
        if (metadata.getChannels() != targetSpec.getMinChannels()) {
            errors.add(String.format("%s仅支持双声道，当前为%s声道",
                    targetSpec.getTypeName(), metadata.getChannels()));
        }

        // 校验失败抛出异常
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(targetSpec.getTypeName() + "规格校验失败：" + String.join("；", errors));
        }
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
    public Result<Void> removeMusic(Long musicId) {

        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId ==  null){
                throw new RuntimeException("请先登录再进行该操作");
            }

            // 校验音乐是否存在（已被删除则直接返回失败）
            if (musicMapper.selectById(musicId) == null) {
                throw new RuntimeException("该音乐已被删除");
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

            // 3. 删除歌单关联信息
            QueryWrapper<PlaylistMusic> playlistQueryWrapper = new QueryWrapper<>();
            playlistQueryWrapper.eq("music_id", musicId);
            playlistMusicMapper.delete(playlistQueryWrapper);

            // 4. 删除点赞信息
            QueryWrapper<LikeRecord> likeRecordQueryWrapper = new QueryWrapper<>();
            likeRecordQueryWrapper.eq("target_id", musicId)
                    .eq("target_type", 1);
            likeRecordMapper.delete(likeRecordQueryWrapper);

            // 6. 删除评论表
            QueryWrapper<Comment> commentQueryWrapper = new QueryWrapper<>();
            commentQueryWrapper.eq("target_id", musicId)
                    .eq("target_type", 1);
            commentMapper.delete(commentQueryWrapper);

            // 7. 删除歌词
            QueryWrapper<MusicLyric> musicLyricQueryWrapper = new QueryWrapper<>();
            musicLyricQueryWrapper.eq("music_id", musicId);
            musicLyricMapper.delete(musicLyricQueryWrapper);

            //  删除音乐主信息
            if (musicMapper.deleteById(musicId) == 0) {
                throw new RuntimeException("音乐删除失败");
            }
            // 删除缓存
            // 音乐详情缓存
            String cacheKey = MusicConstants.CACHE_KEY_MUSIC_DETAIL + musicId;
            stringRedisTemplate.delete(cacheKey);
            // 音乐存在缓存
            String cacheKey2 = RedisCacheConstants.MUSIC_EXIST_KEY_PREFIX + musicId;
            stringRedisTemplate.delete(cacheKey2);
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
     * @param dto 音乐播放DTO，包含音乐ID和音质参数（必填）
     * @param response HTTP响应对象，用于返回文件流和状态信息
     */
    @Override
    public void downloadMusic(MusicPlayDTO dto, HttpServletResponse response) {
        try {
            Long musicId = Long.parseLong(dto.getMusicId());
            Integer quality = dto.getQuality();
            // 1. 校验参数有效性
            if (quality == null) {
                response.setStatus(400); // 参数错误状态码
                response.setHeader("msg", "无效的音质参数");
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

            // 7. 异步更新下载量
            MusicDownloadCountMsg msg = new MusicDownloadCountMsg();
            msg.setMusicId(musicId);
            rabbitTemplate.convertAndSend(
                    RabbitConstants.MUSIC_EXCHANGE,  // 交换机名称
                    RabbitConstants.ROUTING_KEY_MUSIC_DOWNLOAD,  // 路由键
                    musicId  // 消息体
            );

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
    public Result<EchoMusicVO> echoMusic(Long musicId) {

        if (SecurityUtil.getCurrentUserId() ==  null){
            return Result.fail("请先登录再进行该操作");
        }

        try {
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
            EchoMusicVO echoMusicVO = new EchoMusicVO();
            BeanUtils.copyProperties(music, echoMusicVO);

            // 4. 查询该音乐关联的所有音频记录（多音质）
            QueryWrapper<MusicAudio> wrapper = new QueryWrapper<>();
            wrapper.eq("music_id", musicId); // 按音乐ID关联
            List<MusicAudio> audioList = musicAudioMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(audioList)) {
                return Result.fail("未查询到该音乐的音频信息");
            }
            echoMusicVO.setAudioList(audioList); // 给VO设置音频列表（含音质、文件大小等信息）

            // 补充专辑名称（通过专辑ID关联查询）
            Album album = albumMapper.selectById(music.getAlbumId());
            if (album != null) {
                echoMusicVO.setAlbumName(album.getAlbumName());
            }

            // 5. 查询标签
            List<MusicTagVO> tags = musicTagMapper.getTagNamesByMusicId(musicId);
            echoMusicVO.setTags(tags);

            // 歌词
            QueryWrapper<MusicLyric> musicLyricWrapper = new QueryWrapper<>();
            musicLyricWrapper.eq("music_id", musicId);
            MusicLyric musicLyric = musicLyricMapper.selectOne(musicLyricWrapper);
            if (musicLyric != null) {
                echoMusicVO.setLyricUrl(musicLyric.getLyricUrl());
            }

            // 返回成功结果（基础详情VO）
            return Result.ok(echoMusicVO);

        } catch (Exception e) {
            // 返回友好提示（避免暴露技术细节）
            return Result.fail("回显音乐详情失败"+e.getMessage());
        }
    }

    /**
     * 音乐信息更新接口（支持文件更新）
     * 支持音乐基础信息、歌词、封面、音频的部分或全量更新，含事务回滚和文件清理逻辑
     * @param musicId 待更新音乐的主键ID
     * @param musicDTO 音乐更新参数DTO（含基础信息、音质等）
     * @param lyric 新上传的歌词文件（可选，为null则不更新）
     * @param cover 新上传的封面文件（可选，为null则不更新）
     * @param standard 新上传的标准音质文件（可选，为null则不更新）
     * @param highDefinition 新上传的高清音质文件（可选，为null则不更新）
     * @param lossless 新上传的无损音质文件（可选，为null则不更新）
     * @param spatialAudio 新上传的空间音频文件（可选，为null则不更新）
     * @return Result 操作结果（成功/失败提示）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 事务注解：任何异常触发回滚
    public Result<Void> updateMusic(Long musicId, MusicDTO musicDTO,
                                    MultipartFile lyric, MultipartFile cover,
                                    MultipartFile standard,
                                    MultipartFile highDefinition,
                                    MultipartFile lossless,
                                    MultipartFile spatialAudio
                                    ) {

        if (SecurityUtil.getCurrentUserId() == null) {
            return Result.fail("请先登录再进行该操作");
        }
        Map<Integer, MultipartFile> audioMap = new HashMap<>();
        if (standard != null && !standard.isEmpty()){
            audioMap.put(1, standard);
        }
        if (highDefinition != null && !highDefinition.isEmpty()){
            audioMap.put(2, highDefinition);
        }
        if (lossless != null && !lossless.isEmpty()){
            audioMap.put(3, lossless);
        }
        if (spatialAudio != null && !spatialAudio.isEmpty()){
            audioMap.put(4, spatialAudio);
        }

        // 存储新上传的文件路径（用于事务回滚时删除）
        List<String> newUploadedFiles = new ArrayList<>();
        // 存储待删除的旧文件路径（事务成功后删除，避免提前删除导致回滚后文件丢失）
        List<String> oldFilesToDelete = new ArrayList<>();
        // 存储新音频的时长（同一音乐不同音质时长需一致）
        Integer newDuration = null;

        try {
            // 1. 校验音乐ID和原始音乐是否存在
            if (musicId == null || musicId <= 0) {
                throw new RuntimeException("音乐ID无效");
            }
            Music oldMusic = this.getById(musicId);
            if (oldMusic == null) {
                throw new RuntimeException("音乐不存在");
            }

            // 2. 校验DTO参数合法性
            if (!musicDTO.updateValidate()) {
                throw new RuntimeException("音乐参数有误");
            }

            // 3. 初始化文件路径（默认沿用旧文件路径）
            String newCoverUrl = oldMusic.getCoverUrl();

            // 3.1 处理歌词文件更新（仅上传新文件时）
            if (lyric != null && !lyric.isEmpty()) {
                // 删除旧文件
                QueryWrapper<MusicLyric> musicLyricWrapper = new QueryWrapper<>();
                musicLyricWrapper.eq("music_id", musicId);
                MusicLyric oldMusicLyric = musicLyricMapper.selectOne(musicLyricWrapper);
                if (oldMusicLyric == null) {
                    throw new RuntimeException("未查询到该音乐的歌词信息"); // 替换return
                }
                String oldLyricUrl = oldMusicLyric.getLyricUrl();
                oldFilesToDelete.add(oldLyricUrl);

                // 解析新歌词
                if (!minIoService.isAllowedFile(lyric, "lyric") || lyric.getSize() > SystemConstants.MAX_LYRIC_SIZE) {
                    throw new RuntimeException("歌词文件格式或大小不符合要求"); // 替换return
                }
                String lyricUrl = null;
                String lyricContent = null;
                String lyricType = null;
                // 2.2 识别歌词格式（从文件名后缀提取）
                lyricType = getLyricType(lyric.getOriginalFilename());
                // 2.3 上传歌词文件到MinIO，获取地址
                lyricUrl = minIoService.uploadFile(lyric);
                if (lyricUrl == null) {
                    throw new RuntimeException("歌词文件上传失败");
                }
                newUploadedFiles.add(lyricUrl);
                // 2.4 读取歌词原始内容（存入数据库，避免后续解析需下载文件）
                lyricContent = new String(lyric.getBytes(), StandardCharsets.UTF_8);
                // 清理歌词内容中的特殊字符（可选，避免解析异常）
                lyricContent = cleanLyricContent(lyricContent);

                //删除旧歌词，更新新歌词
                if (musicLyricMapper.delete(musicLyricWrapper) == 0) {
                    throw new RuntimeException("歌词删除失败");
                }
                MusicLyric musicLyric = new MusicLyric();
                musicLyric.setMusicId(musicId);
                musicLyric.setLyricUrl(lyricUrl);
                musicLyric.setLyricContent(lyricContent);
                musicLyric.setLyricType(lyricType);
                if (musicLyricMapper.insert(musicLyric) != 1) {
                    throw new RuntimeException("歌词信息保存失败");
                }
            }

            // 3.2 处理封面文件更新（仅上传新文件时）
            if (cover != null && !cover.isEmpty()) {
                if (!minIoService.isAllowedFile(cover, "picture") || cover.getSize() > SystemConstants.MAX_PICTURE_SIZE) {
                    throw new RuntimeException("封面文件格式或大小不符合要求"); // 替换return
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
                        throw new RuntimeException("音质类型不合法：" + quality); // 替换return
                    }
                    // 校验音频文件格式和大小
                    if (!minIoService.isAllowedFile(audio, "music") || audio.getSize() > SystemConstants.MAX_MUSIC_SIZE) {
                        throw new RuntimeException("音频文件（音质：" + quality + "）格式或大小不符合要求"); // 替换return
                    }

                    // 上传新音频文件
                    String newAudioUrl = uploadAudio(audio, quality);
                    if (newAudioUrl == null) {
                        throw new RuntimeException("音频文件（音质：" + quality + "）上传失败");
                    }
                    newUploadedFiles.add(newAudioUrl);

                    // 计算并校验音频时长（同一音乐所有音质时长需一致）
                    int duration = AudioDurationUtil.getAudioDuration(audio);
                    if (duration == -1) {
                        throw new RuntimeException("音频（音质：" + quality + "）时长计算错误");
                    }
                    if (newDuration == null) {
                        newDuration = duration; // 首次计算时长，作为基准
                    } else if (!newDuration.equals(duration)) {
                        throw new RuntimeException("不同音质的音频时长不一致"); // 替换return
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
            updateMusic.setCoverUrl(newCoverUrl);
            updateMusic.setStatus(0); // 重置为待审核状态
            // 音频更新时同步更新时长，未更新则沿用旧时长
            updateMusic.setDuration(newDuration != null
                    ? newDuration
                    : oldMusic.getDuration());
            // 保留系统维护字段（播放量、下载量不允许修改）
            updateMusic.setPlayCount(oldMusic.getPlayCount());
            updateMusic.setDownloadCount(oldMusic.getDownloadCount());

            // 5. 更新音乐主表信息
            if (!this.updateById(updateMusic)) {
                throw new RuntimeException("音乐主信息更新失败");
            }

            // 删除缓存
            // 音乐详情缓存
            String cacheKey = MusicConstants.CACHE_KEY_MUSIC_DETAIL + musicId;
            stringRedisTemplate.delete(cacheKey);
            // 音乐存在缓存
            String cacheKey2 = RedisCacheConstants.MUSIC_EXIST_KEY_PREFIX + musicId;
            stringRedisTemplate.delete(cacheKey2);

            // 6. 事务成功后，删除所有旧文件（歌词、封面、旧音频）
            if (!oldFilesToDelete.isEmpty()) {
                minIoService.batchDeleteFiles(oldFilesToDelete);
            }

            return Result.ok(); // 仅保留最终成功的return

        } catch (Exception e) {
            // 7. 异常回滚：删除新上传的所有文件，避免垃圾文件
            if (!newUploadedFiles.isEmpty()) {
                minIoService.batchDeleteFiles(newUploadedFiles);
            }
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail("修改音乐失败，请稍后重试：" + e.getMessage());
        }
    }

    /**
     * 音乐完整详情查询接口（面向前端）
     * @param musicId 音乐主键ID
     * @return Result<MusicDetailVO> 完整详情VO（含关联信息和用户交互状态）
     */
    @Override
    public Result<MusicDetailVO> getOneMusic(Long musicId){
        try {
            // 1. 校验音乐ID有效性
            if (musicId == null || musicId <= 0) {
                return Result.fail("音乐ID无效");
            }

            // 2. 构建缓存 Key
            String cacheKey = MusicConstants.CACHE_KEY_MUSIC_DETAIL + musicId;
            String lockKey = RedisCacheConstants.LOCK_KEY_MUSIC_DETAIL + musicId;

            // 3. 先查 Redis 缓存（手动反序列化）
            String cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheJson != null) {
                if (cacheJson.isEmpty()) { // 空值缓存（防止穿透）
                    return Result.fail("音乐不存在");
                }
                // 缓存命中：刷新过期时间（续命）
                stringRedisTemplate.expire(cacheKey, MusicConstants.CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                // 手动将 JSON 字符串解析为 MusicDetailVO（FastJSON2 实现）
                MusicDetailVO cacheVO = JSON.parseObject(cacheJson, MusicDetailVO.class);
                return Result.ok(cacheVO);
            }

            // 4. 缓存未命中，获取分布式锁（只允许一个线程查DB）
            RLock lock = redissonClient.getLock(lockKey);
            boolean isLockAcquired = false;
            try {
                // 尝试获取锁：最多等待 3 秒，获取成功后 10 秒自动释放
                isLockAcquired = lock.tryLock(RedisCacheConstants.LOCK_WAIT_TIME, RedisCacheConstants.LOCK_EXPIRE_TTL, TimeUnit.SECONDS);
                if (!isLockAcquired) {
                    // 未获取到锁（其他线程正在重建缓存），返回友好提示或过期缓存
                    return Result.fail("当前查询人数较多，请稍后重试");
                }

                // 5. 第二次查缓存（双重检查：防止其他线程已重建缓存）
                cacheJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cacheJson != null) {
                    if (cacheJson.isEmpty()) {
                        return Result.fail("音乐不存在");
                    }
                    MusicDetailVO cacheVO = JSON.parseObject(cacheJson, MusicDetailVO.class);
                    return Result.ok(cacheVO);
                }

                // 6. 锁内查 DB 聚合数据（只有一个线程执行，避免 DB 压力）
                MusicVO musicVO = getMusicVO(musicId);
                if (musicVO == null) {
                    // 缓存空值（5分钟过期，防止穿透）
                    stringRedisTemplate.opsForValue().set(cacheKey, "", RedisCacheConstants.CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    return Result.fail("音乐不存在");
                }

                MusicDetailVO musicDetailVO = new MusicDetailVO();
                BeanUtils.copyProperties(musicVO, musicDetailVO);

                // 查询标签
                List<MusicTagVO> tags = musicTagMapper.getTagNamesByMusicId(musicId);
                musicDetailVO.setTags(tags);

                // 查询歌词
                QueryWrapper<MusicLyric> musicLyricWrapper = new QueryWrapper<>();
                musicLyricWrapper.eq("music_id", musicId);
                MusicLyric musicLyric = musicLyricMapper.selectOne(musicLyricWrapper);
                if (musicLyric != null) {
                    musicDetailVO.setLyricUrl(musicLyric.getLyricUrl());
                    musicDetailVO.setLyricType(musicLyric.getLyricType());

                    // 区分歌词类型处理
                    String lyricType = musicLyric.getLyricType().toLowerCase();
                    if ("txt".equals(lyricType)) {
                        // TXT类型：赋值纯文本，同步歌词置空
                        String rawTxt = LyricParseUtil.parseTxtToRawText(musicLyric.getLyricContent());
                        musicDetailVO.setRawLyricText(rawTxt);
                        musicDetailVO.setSyncLyric(new ArrayList<>());
                    } else {
                        // LRC/KRC类型：解析同步歌词，纯文本置空
                        List<LyricLineDTO> syncLyric = LyricParseUtil.parse(
                                musicLyric.getLyricContent(),
                                musicLyric.getLyricType()
                        );
                        musicDetailVO.setSyncLyric(syncLyric);
                        musicDetailVO.setRawLyricText("");
                    }
                }

                // 7. 写入缓存（设置 1 小时过期）
                String jsonStr = JSON.toJSONString(musicDetailVO);
                stringRedisTemplate.opsForValue().set(cacheKey, jsonStr, MusicConstants.CACHE_TTL_SECONDS, TimeUnit.SECONDS);

                return Result.ok(musicDetailVO);
            } finally {
                // 8. 释放锁（必须在 finally 中执行，避免锁泄露）
                if (isLockAcquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            return Result.fail("查询音乐详情失败：" + e.getMessage());
        }
    }

    //搜索单曲
    @Override
    public PageResp<MusicVO> searchMusic(SearchDTO dto) {
        // 1. 校验关键词
        if (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty()) {
            return new PageResp<>();
        }
        String keyword = "%" + dto.getKeyword().trim() + "%"; // 模糊匹配前缀后缀

        // 2. 构建分页参数
        Page<Music> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 3. 构建多条件查询（音乐名、标签名、音乐人名字 模糊匹配）
        QueryWrapper<Music> queryWrapper = new QueryWrapper<>();

        queryWrapper.and(wrapper -> wrapper
                        .like("m.music_name", keyword)        // 条件1：音乐名
                        .or().like("mu.stage_name", keyword)  // 条件2：音乐人
                        .or().like("mt.tag_name", keyword)    // 条件3：标签名
                )
                .eq("m.status", 1); // 全局过滤：所有条件都需满足状态=1

        // 4. 执行多表联查+分页+排序（SQL已含ORDER BY play_count DESC）
        IPage<Music> musicPage = musicMapper.searchMusicByMultiCondition(page, queryWrapper);

        // 5. 转换为MusicDetailVO并填充关联信息（专辑名、点赞状态、音频列表等）
        List<MusicVO> voList = new ArrayList<>();
        for (Music music : musicPage.getRecords()) {
            MusicVO vo = getMusicVO(music.getId());
            voList.add(vo);
        }

        // 6. 封装分页结果
        PageResp<MusicVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(musicPage.getTotal());
        pageResult.setPages(musicPage.getPages());

        return pageResult;
    }

    //搜索专辑
    @Override
    public PageResp<SearchAlbumVO> searchAlbum(SearchDTO dto) {
        // 1. 校验关键词
        if (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty()) {
            return new PageResp<>();
        }
        String keyword = "%" + dto.getKeyword().trim() + "%"; // 模糊匹配前缀后缀

        // 2. 构建分页参数
        Page<AlbumVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 4. 执行多表联查+分页+排序（SQL已含ORDER BY play_count DESC）
        IPage<AlbumVO> albumPage = albumMapper.searchAlbumByKey(page, keyword);

        // 5. 转换为SearchAlbumVO并填充关联信息（专辑名、音乐人名字等）
        List<SearchAlbumVO> voList = new ArrayList<>();
        for (AlbumVO album : albumPage.getRecords()) {
            SearchAlbumVO vo = new SearchAlbumVO();
            BeanUtils.copyProperties(album, vo);

            voList.add(vo);
        }

        // 6. 封装分页结果
        PageResp<SearchAlbumVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(albumPage.getTotal());
        pageResult.setPages(albumPage.getPages());

        return pageResult;
    }

    @Override
    public Result<MusicPlayVO> playMusic(MusicPlayDTO dto) {
        try {
            // 从DTO中提取音乐ID和音质类型
            Long musicId = Long.parseLong(dto.getMusicId());
            Integer quality = dto.getQuality();
            // 1. 参数校验
            // 音乐ID非空且为正数
            if (musicId <= 0) {
                return Result.fail("音乐ID出错");
            }
            // 音质类型合法（1-4）
            if (!(quality == 1 || quality == 2 || quality == 3 || quality == 4)) {
                return Result.fail("音质类型出错");
            }

            // 2. 校验音乐是否存在且已上架
            Music music = musicMapper.selectById(musicId);
            if (music == null) {
                return Result.fail("音乐不存在");
            }
            if (music.getStatus() != 1) { // 1-上架状态，其他状态（0-待审核、2-下架）不可播放
                return Result.fail("该音乐暂不支持播放");
            }

            if (music.getIsVip() == 1){
                QueryWrapper<Vip> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", SecurityUtil.getCurrentUserId())
                        .gt("end_time", new Date());
                Vip vip = vipMapper.selectOne(queryWrapper);
                if (vip == null) {
                    return Result.fail("请先充值VIP再播放");
                }
            }

            // 3. 校验该音质的音频是否存在且文件有效
            QueryWrapper<MusicAudio> audioWrapper = new QueryWrapper<>();
            audioWrapper.eq("music_id", musicId)
                    .eq("quality_type", quality);
            MusicAudio musicAudio = musicAudioMapper.selectOne(audioWrapper);
            if (musicAudio == null) {
                return Result.fail("不存在该音质的音频");
            }
            // 校验MinIO中音频文件是否存在
            if (!minIoService.isObjectExists(musicAudio.getAudioUrl())) {
                return Result.fail("音频文件已失效");
            }

            // 4. 生成可直接播放的音频URL（MinIO预签名URL，避免暴露原始路径）
            String playUrl = minIoService.generatePresignedUrl(musicAudio.getAudioUrl(), 3600); // 1小时有效
            if (playUrl == null) {
                return Result.fail("播放链接生成失败");
            }

            // 组装返回VO
            MusicPlayVO playVO = new MusicPlayVO();
            MusicVO vo = getMusicVO(musicId);
            BeanUtils.copyProperties(vo, playVO);
            playVO.setAudioUrl(playUrl);

            return Result.ok(playVO);
        }catch (Exception e){
            return Result.fail("播放失败"+e.getMessage());
        }
    }

    @Override
    public MusicVO getMusicVO(Long musicId) {
        MusicVO musicVO = new MusicVO();
        Music music = musicMapper.selectById(musicId);
        if (music == null){
            return null;
        }
        BeanUtils.copyProperties(music, musicVO);

        // 时长
        musicVO.setDuration(AudioDurationUtil.formatSecondsToMS(music.getDuration()));

        // 补充专辑名称（通过专辑ID关联查询）
        Album album = albumMapper.selectById(musicVO.getAlbumId());
        if (album != null) {
            musicVO.setAlbumName(album.getAlbumName());
        }

        // 补充音乐人名称（通过音乐人ID关联查询，取艺名）
        Musician musician = musicianMapper.selectById(musicVO.getMusicianId());
        if (musician != null) {
            musicVO.setMusicianName(musician.getStageName());
        }

        //  判断是否点赞
        QueryWrapper<LikeRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", SecurityUtil.getCurrentUserId()) // 获取当前登录用户ID
                .eq("target_type", 1) // 目标类型：1=音乐（系统定义）
                .eq("target_id", musicId); // 目标ID：音乐ID
        LikeRecord likeRecord = likeRecordMapper.selectOne(wrapper);
        musicVO.setIsLiked(likeRecord != null ? 1 : 0);

        // 关联音频类型列表（通过musicId查music_audio表）
        QueryWrapper<MusicAudio> audioWrapper = new QueryWrapper<>();
        audioWrapper.eq("music_id", music.getId());
        List<MusicAudio> audioList = musicAudioMapper.selectList(audioWrapper);
        List<String> audioTypeList = new ArrayList<>();
        if (!audioList.isEmpty()) {
            for (MusicAudio musicAudio : audioList) {
                if (musicAudio.getQualityType() == 1) {
                    audioTypeList.add("标准");
                } else if (musicAudio.getQualityType() == 2) {
                    audioTypeList.add("高清");
                } else if (musicAudio.getQualityType() == 3) {
                    audioTypeList.add("无损");
                } else if (musicAudio.getQualityType() == 4) {
                    audioTypeList.add("空间音频");
                }
            }
        } else {
            musicVO.setAudioList(Collections.emptyList());
        }
        musicVO.setAudioList(audioTypeList);

        return musicVO;
    }

    /**
     * 上报播放状态，达标则统计播放量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reportPlayStatus(MusicPlayReportDTO dto) {
        try {
            // 1. 参数校验
            // 兜底校验（防止未通过注解校验的情况）
            if (dto.getPlayedDuration() > dto.getTotalDuration()) {
                return Result.fail("有效播放时长不能超过歌曲总时长");
            }

            // 2. 校验歌曲存在
            if (!exists(dto.getMusicId())) { // 改用缓存，避免查库
                return Result.fail("歌曲不存在");
            }

            // 3. 过滤无效上报
            if (dto.getPlayedDuration() < MusicConstants.MIN_PLAY_SECONDS) {
                return Result.ok(); // 过滤无效上报，不统计播放量
            }

            // 4. 核心写入
            int threshold = (int) (dto.getTotalDuration() * MusicConstants.MIN_PLAY_RATIO);
            int listenCount = threshold <= dto.getPlayedDuration() ? 1 : 0;

            // 使用Redisson分布式锁
            RLock lock = redissonClient.getLock("listen_history:" + dto.getUserId() + ":" + dto.getMusicId());
            try {
                // 加锁（3秒超时，避免死锁）
                boolean locked = lock.tryLock(RedisCacheConstants.LOCK_WAIT_TIME, RedisCacheConstants.LOCK_EXPIRE_TTL, TimeUnit.SECONDS);
                if (!locked) {
                    return Result.fail("系统繁忙，请稍后重试");
                }

                // 执行insertOrUpdate
                userListenHistoryMapper.insertOrUpdateListenHistory(
                        dto.getUserId(), dto.getMusicId(), dto.getPlayedDuration(),
                        listenCount, LocalDate.now()
                );

                // 存储歌曲标签听歌记录
                if (listenCount > 0) {
                    // 查询歌曲标签
                    List<MusicTagVO> tagNames = musicTagMapper.getTagNamesByMusicId(dto.getMusicId());

                    if (!tagNames.isEmpty()) {
                        List<MusicTagVO> tagList = musicTagMapper.getTagNamesByMusicId(dto.getMusicId());
                        // 构建MQ消息
                        MusicPlayReportMqMessage mqMessage = new MusicPlayReportMqMessage();
                        mqMessage.setUserId(dto.getUserId());
                        mqMessage.setMusicId(dto.getMusicId());
                        mqMessage.setListenCount(listenCount);
                        mqMessage.setTagList(tagList);
                        mqMessage.setCurrentDate(LocalDate.now());

                        // 发送MQ消息（异步执行标签Redis写入）
                        rabbitTemplate.convertAndSend(RabbitConstants.MUSIC_EXCHANGE, RabbitConstants.ROUTING_KEY_MUSIC_PLAY, mqMessage);
                    }
                }
            } finally {
                // 释放锁
                if (lock != null && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

            return Result.ok();

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            // 统一返回失败结果
            return Result.fail("播放状态上报失败，请稍后重试：" + e.getMessage());
        }
    }
    /**
     * 校验歌曲是否存在（优先查缓存）
     * @param musicId 歌曲ID
     * @return true=存在，false=不存在
     */
    public boolean exists(Long musicId) {
        if (musicId == null) {
            return false;
        }
        String cacheKey = RedisCacheConstants.MUSIC_EXIST_KEY_PREFIX + musicId;

        // 1. 查缓存
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if ("1".equals(cacheValue)) { // 缓存命中：存在
            return true;
        }
        if ("0".equals(cacheValue)) { // 缓存命中：不存在（空值缓存）
            return false;
        }

        // 2. 缓存未命中，加分布式锁防击穿（同一歌曲只查一次库）
        String lockKey = "music_exist_lock:" + musicId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(RedisCacheConstants.LOCK_WAIT_TIME, RedisCacheConstants.LOCK_EXPIRE_TTL, TimeUnit.SECONDS);
            if (!locked) {
                // 加锁失败，重试查缓存（可能其他线程已写入）
                cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
                return "1".equals(cacheValue);
            }

            // 3. 锁内再次查缓存（双重检查）
            cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if ("1".equals(cacheValue)) {
                return true;
            }
            if ("0".equals(cacheValue)) {
                return false;
            }

            // 4. 查数据库
            boolean isExist = musicMapper.selectById(musicId) != null;

            // 5. 写入缓存
            if (isExist) {
                // 优化：歌曲存在的缓存设置可配置，且预留主动失效接口
                stringRedisTemplate.opsForValue().set(cacheKey, "1", RedisCacheConstants.CACHE_LONG_TTL, TimeUnit.SECONDS);
            } else {
                // 空值缓存时间缩短，比如30秒
                stringRedisTemplate.opsForValue().set(cacheKey, "0", RedisCacheConstants.LOCK_TTL, TimeUnit.SECONDS);
            }
            return isExist;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 降级：直接查库（避免缓存击穿导致服务不可用）
            return musicMapper.selectById(musicId) != null;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 按标签查询歌曲（分页查询）
     * @param tagIds 标签ID列表
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 分页歌曲VO列表
     */
    @Override
    public Result<PageResp<MusicVO>> filterMusicByTags(List<Long> tagIds, int pageNum, int pageSize){
        // 1. 参数校验
        if (CollectionUtils.isEmpty(tagIds)) {
            return Result.fail("标签不能为空");
        }

        // 2. 分页查询
        Page<Music> page = new Page<>(pageNum, pageSize);
        IPage<Music> musicIPage = musicMapper.selectMusicsByTagIds(page, tagIds, tagIds.size());

        // 3. 转换VO
        List<MusicVO> voList = new ArrayList<>();
        for (Music music : musicIPage.getRecords()) {
            MusicVO musicVO = getMusicVO(music.getId());
            voList.add(musicVO);
        }

        // 4. 封装分页结果
        PageResp<MusicVO> resp = new PageResp<>();
        resp.setRecords(voList);
        resp.setTotal(musicIPage.getTotal());
        resp.setPages(musicIPage.getPages());

        // 5. 仅保留成功返回
        return Result.ok(resp);
    }

}