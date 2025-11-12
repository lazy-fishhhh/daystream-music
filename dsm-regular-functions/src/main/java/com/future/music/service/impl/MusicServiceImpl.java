package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.constants.SystemConstants;
import com.future.music.entity.dto.MusicDTO;
import com.future.music.entity.pojo.music.Music;
import com.example.music.entity.pojo.music.MusicAudio;
import com.future.music.entity.pojo.util.Result;
import com.future.music.mapper.MusicAudioMapper;
import com.future.music.mapper.MusicMapper;
import com.future.music.service.IMinIoService;
import com.future.music.service.IMusicService;

import constants.SystemConstants;
import entity.dto.MusicDTO;
import entity.pojo.music.Music;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import util.AudioDurationUtil;

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
    @Transactional
    public Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover, MultipartFile audio) {
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
        String lyricName = minIoService.uploadFile(lyric);      // 歌词文件上传后的存储名/路径
        String coverName = minIoService.uploadFile(cover);      // 封面文件上传后的存储名/路径
        String audioName = minIoService.uploadFile(audio);      // 音频文件上传后的存储名/路径

        // 校验文件上传是否成功
        if (lyricName == null || coverName == null || audioName == null) {
            return Result.fail("文件上传失败");
        }

        // 5. 计算音频时长（注意：原代码传入lyric为笔误，已修正为audio）
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
        try {
            // 调用MyBatis-Plus的save方法保存，失败则抛异常触发回滚
            if (!this.save(music)) {
                throw new RuntimeException("音乐保存失败");
            }
        } catch (Exception e) {
            // 手动标记事务回滚（因异常被捕获，需显式触发回滚）
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(e.getMessage());  // 返回失败信息
        }

        // 8. 构建音乐音频关联实体（一个音乐可对应多个音质的音频）
        MusicAudio musicAudio = new MusicAudio();
        musicAudio.setAudioUrl(audioName);         // 设置音频文件路径
        musicAudio.setMusicId(music.getId());      // 关联音乐主键ID
        musicAudio.setQualityType(musicDTO.getQuality());  // 设置音频音质类型
        musicAudio.setFileSize(audio.getSize());   // 设置音频文件大小

        // 9. 保存音乐音频关联信息到数据库
        try {
            // 插入成功返回受影响行数，若不为1则抛异常触发回滚
            if (musicAudioMapper.insert(musicAudio) != 1) {
                throw new RuntimeException("音乐音频保存失败");
            }
        } catch (Exception e) {
            // 手动标记事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(e.getMessage());  // 返回失败信息
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
    @Transactional
    public Result removeMusic(Long musicId) {
        // 校验音乐是否存在（已被删除则直接返回失败）
        if (musicMapper.selectById(musicId) == null) {
            return Result.fail("该音乐已被删除");
        }

        try {
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


}