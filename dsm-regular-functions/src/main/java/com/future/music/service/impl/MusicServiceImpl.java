package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.mapper.MusicAudioMapper;
import com.future.music.mapper.MusicMapper;
import com.future.music.service.IMinIoService;

import com.future.music.service.IMusicService;
import constants.SystemConstants;
import entity.dto.MusicDTO;
import entity.pojo.music.Music;
import entity.pojo.music.MusicAudio;
import entity.pojo.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import util.AudioDurationUtil;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 保存音乐信息（包含音乐主信息、音频、歌词、封面）
     * 标注 @Transactional 确保所有数据库操作在同一事务中，保证数据一致性
     *
     * @param musicDTO 音乐基本信息DTO（包含歌名、歌手等非文件信息）
     * @param lyric    歌词文件
     * @param cover    封面图片文件
     * @param audio    音频文件
     * @return 处理结果（成功/失败信息）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, MultipartFile audio) {
        List<String> uploadedFilePaths = new ArrayList<>(); // 用于存储已上传文件路径，供回滚删除

        try {
            // 1. 校验音乐DTO参数合法性（如必填字段是否为空等）
            if (!musicDTO.validate()) {
                return Result.fail("音乐参数有错");
            }

            // 2. 校验文件格式（歌词需符合音乐文件格式，封面需符合图片格式）
            if (!minIoService.isAllowedFile(lyric, "music") || !minIoService.isAllowedFile(cover, "picture")) {
                return Result.fail("文件格式不正确");
            }

            // 3. 校验文件大小（歌词不超过音乐文件上限，封面不超过图片上限）
            if (lyric.getSize() > SystemConstants.MAX_MUSIC_SIZE || cover.getSize() > SystemConstants.MAX_PICTURE_SIZE) {
                return Result.fail("文件大小超出限制");
            }

            // 4. 上传文件到MinIO存储（歌词、封面、音频）
            String lyricName = minIoService.uploadFile(lyric);
            if (lyricName != null) uploadedFilePaths.add(lyricName);
            String coverName = minIoService.uploadFile(cover);
            if (coverName != null) uploadedFilePaths.add(coverName);
            String audioName = minIoService.uploadFile(audio);
            if (audioName != null) uploadedFilePaths.add(audioName);

            // 校验文件上传是否成功
            if (lyricName == null || coverName == null || audioName == null) {
                return Result.fail("文件上传失败");
            }

            // 5. 计算音频时长
            long duration = AudioDurationUtil.getAudioDuration(audio);
            if (duration == -1) {
                return Result.fail("音乐时长计算错误");
            }

            // 6. 构建音乐实体并设置关联文件路径
            Music music = musicDTO.toEntity();  // 将DTO转换为实体对象
            music.setCoverUrl(coverName);       // 设置封面图片路径
            music.setLyricUrl(lyricName);       // 设置歌词文件路径
            music.setDuration(AudioDurationUtil.formatSecondsToMS(duration));        // 设置音频时长

            // 7. 保存音乐主信息到数据库
            // 调用MyBatis-Plus的save方法保存，失败则抛异常触发回滚
            if (!this.save(music)) {
                throw new RuntimeException("音乐保存失败");
            }

            // 8. 构建音乐音频关联实体（一个音乐可对应多个音质的音频）
            MusicAudio musicAudio = new MusicAudio();
            musicAudio.setAudioUrl(audioName);         // 设置音频文件路径
            musicAudio.setMusicId(music.getId());      // 关联音乐主键ID
            musicAudio.setQualityType(musicDTO.getQuality());  // 设置音频音质类型
            musicAudio.setFileSize(audio.getSize());   // 设置音频文件大小

            // 9. 保存音乐音频关联信息到数据库
            // 插入成功返回受影响行数，若不为1则抛异常触发回滚
            if (musicAudioMapper.insert(musicAudio) != 1) {
                throw new RuntimeException("音乐音频保存失败");
            }
        }catch (Exception e) {
            // 事务回滚时，删除MinIO中已上传的文件（核心新增逻辑）
            if (!uploadedFilePaths.isEmpty()) {
                minIoService.batchDeleteFiles(uploadedFilePaths);
            }
            return Result.fail("保存音乐失败，请稍后重试");
        }

        // 所有操作成功，返回成功结果
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

            // 1. 先删除关联的音频信息（根据音乐ID查询并删除）
            QueryWrapper<MusicAudio> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("music_id", musicId);  // 条件：music_id = 传入的音乐ID
            // 执行删除并校验结果，若受影响行数为0则抛异常
            if (musicAudioMapper.delete(queryWrapper) == 0) {
                throw new RuntimeException("音乐音频删除失败");
            }

            // 2. 再删除音乐主信息
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

    @Override
    public void downloadMusic(Long musicId, Integer quality, HttpServletResponse response) {

        try {
            if (musicId == null) {
                response.setStatus(400);
                response.setHeader("msg", "无效的ID");
                return;
            }
            Music music = getById(musicId);
            if (music == null) {
                response.setStatus(404);
                response.setHeader("msg", "音乐不存在");
                return;
            }

            QueryWrapper<MusicAudio> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("music_id", musicId)
                    .eq("quality_type", quality);
            MusicAudio musicAudio = musicAudioMapper.selectOne(queryWrapper);
            if (musicAudio == null) {
                response.setStatus(400);
                response.setHeader("msg", "不存在该音质的音频");
                return;
            }

            if (!minIoService.isObjectExists(musicAudio.getAudioUrl())){
                response.setStatus(404);
                response.setHeader("msg","音频文件不存在或已被删除");
            }

            response.setStatus(200); // 成功状态码
            response.setHeader("msg", "下载成功"); // 响应头携带信息

            minIoService.downloadFile(musicAudio.getAudioUrl(), response);

            music.setDownloadCount(music.getDownloadCount() + 1);
            if (!this.updateById(music))
                throw new RuntimeException("更新音乐下载数失败");

        } catch (Exception e) {
            response.setStatus(500);
            response.setHeader("msg", "下载过程出错：" + e.getMessage());
        }
    }

    @Override
    public Result<Music> echoMusic(Long musicId) {
        if (musicId == null || musicId <= 0) {
            return Result.fail("音乐ID无效");
        }
        Music music = this.getById(musicId);
        if (music == null) {
            return Result.fail("音乐不存在");
        }

        QueryWrapper<MusicAudio> wrapper = new QueryWrapper<>();
        wrapper.eq("music_id", musicId);
        List<MusicAudio> audioList = musicAudioMapper.selectList(wrapper);
        music.setAudioList(audioList); // 关联的音频列表（含音质、文件大小）

        return Result.ok(music);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateMusic(Long musicId, MusicDTO musicDTO,
                              MultipartFile lyric, MultipartFile cover, MultipartFile audio) {
        // 存储新上传的文件（用于回滚）和待删除的旧文件
        List<String> newUploadedFiles = new ArrayList<>();
        List<String> oldFilesToDelete = new ArrayList<>();

        try {
            // 1. 校验音乐ID和原始音乐是否存在
            if (musicId == null || musicId <= 0) {
                return Result.fail("音乐ID无效");
            }
            Music oldMusic = this.getById(musicId);
            if (oldMusic == null) {
                return Result.fail("音乐不存在");
            }

            // 2. 校验DTO参数（复用新增时的validate，但允许部分字段更新）
            // 注意：修改时DTO的非空校验可放宽（如仅校验传入的字段），此处简化为全量校验
            if (!musicDTO.validate()) {
                return Result.fail("音乐参数有误");
            }

            // 3. 处理文件更新（歌词、封面、音频，支持部分更新）
            String newLyricUrl = oldMusic.getLyricUrl(); // 默认沿用旧文件
            String newCoverUrl = oldMusic.getCoverUrl();
            String newAudioUrl = null;
            long newDuration = -1; // 新音频时长（仅音频更新时有效）

            // 3.1 处理歌词文件更新（若上传了新歌词）
            if (lyric != null && !lyric.isEmpty()) {
                // 校验文件格式和大小
                if (!minIoService.isAllowedFile(lyric, "music") || lyric.getSize() > SystemConstants.MAX_MUSIC_SIZE) {
                    return Result.fail("歌词文件格式或大小不符合要求");
                }
                // 上传新文件
                newLyricUrl = minIoService.uploadFile(lyric);
                if (newLyricUrl == null) {
                    throw new RuntimeException("歌词文件上传失败");
                }
                newUploadedFiles.add(newLyricUrl); // 记录新文件（回滚用）
                oldFilesToDelete.add(oldMusic.getLyricUrl()); // 记录旧文件（后续删除）
            }

            // 3.2 处理封面文件更新（若上传了新封面）
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

            // 3.3 处理音频文件更新（若上传了新音频）
            if (audio != null && !audio.isEmpty()) {
                // 校验音频格式
                if (!minIoService.isAllowedFile(audio, "music")) {
                    return Result.fail("音频文件格式不符合要求");
                }
                // 上传新音频
                newAudioUrl = minIoService.uploadFile(audio);
                if (newAudioUrl == null) {
                    throw new RuntimeException("音频文件上传失败");
                }
                newUploadedFiles.add(newAudioUrl);
                // 计算新音频时长
                newDuration = AudioDurationUtil.getAudioDuration(audio);
                if (newDuration == -1) {
                    throw new RuntimeException("新音频时长计算错误");
                }
                // 记录旧音频文件（后续删除）
                QueryWrapper<MusicAudio> audioWrapper = new QueryWrapper<>();
                audioWrapper.eq("music_id", musicId)
                        .eq("quality_type", musicDTO.getQuality());
                MusicAudio oldAudio = musicAudioMapper.selectOne(audioWrapper);
                if (oldAudio != null) {
                    oldFilesToDelete.add(oldAudio.getAudioUrl());
                }
            }

            // 4. 构建更新后的音乐实体
            Music updateMusic = musicDTO.toEntity(); // 从DTO复制基础字段
            updateMusic.setId(musicId); // 绑定ID，确保更新而非新增
            updateMusic.setLyricUrl(newLyricUrl); // 更新歌词路径（可能是旧的）
            updateMusic.setCoverUrl(newCoverUrl); // 更新封面路径
            updateMusic.setDuration(oldMusic.getDuration()); // 默认沿用旧时长
            updateMusic.setStatus(0); //更新状态(待审核)
            // 若音频更新，同步更新时长
            if (newDuration != -1) {
                updateMusic.setDuration(AudioDurationUtil.formatSecondsToMS(newDuration));
            }
            // 保留不变的字段（播放量、下载量、状态等由系统维护，不允许手动修改）
            updateMusic.setPlayCount(oldMusic.getPlayCount());
            updateMusic.setDownloadCount(oldMusic.getDownloadCount());

            // 5. 更新音乐主表
            if (!this.updateById(updateMusic)) {
                throw new RuntimeException("音乐主信息更新失败");
            }

            // 6. 处理音频关联表（MusicAudio）
            if (newAudioUrl != null) {
                // 查询对应音质的旧音频记录
                QueryWrapper<MusicAudio> wrapper = new QueryWrapper<>();
                wrapper.eq("music_id", musicId)
                        .eq("quality_type", musicDTO.getQuality());
                MusicAudio oldAudio = musicAudioMapper.selectOne(wrapper);

                MusicAudio updateAudio = new MusicAudio();
                updateAudio.setAudioUrl(newAudioUrl);
                updateAudio.setMusicId(musicId);
                updateAudio.setQualityType(musicDTO.getQuality());
                updateAudio.setFileSize(audio.getSize());

                if (oldAudio != null) {
                    // 存在旧记录则更新
                    updateAudio.setId(oldAudio.getId());
                    if (musicAudioMapper.updateById(updateAudio) != 1) {
                        throw new RuntimeException("音频信息更新失败");
                    }
                } else {
                    // 不存在则新增（如首次添加该音质）
                    if (musicAudioMapper.insert(updateAudio) != 1) {
                        throw new RuntimeException("新增音频记录失败");
                    }
                }
            }

            // 7. 事务成功后，删除旧文件（避免提前删除导致回滚后文件丢失）
            if (!oldFilesToDelete.isEmpty()) {
                minIoService.batchDeleteFiles(oldFilesToDelete);
            }

            return Result.ok();

        } catch (Exception e) {
            // 回滚：删除本次新上传的文件（避免垃圾文件）
            if (!newUploadedFiles.isEmpty()) {
                minIoService.batchDeleteFiles(newUploadedFiles);
            }
            return Result.fail("修改音乐失败，请稍后重试");
        }
    }

    @Override
    public Result<Music> getOneMusic(Long musicId){
        Result<Music> result= echoMusic(musicId);
        if (result.getErrorMsg() != null) {
            return result;
        }

        Music music = result.getData();


        return Result.ok(music);
    }

}