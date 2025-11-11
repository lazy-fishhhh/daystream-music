package com.future.music.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.constants.SystemConstants;
import com.future.music.entity.dto.MusicDTO;
import com.future.music.entity.pojo.music.Music;
import com.future.music.entity.pojo.util.Result;
import com.future.music.mapper.MusicMapper;
import com.future.music.service.IMinIoService;
import com.future.music.service.IMusicService;
import com.future.music.util.AudioDurationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MusicServiceImpl extends ServiceImpl<MusicMapper, Music> implements IMusicService {

    @Autowired
    private IMinIoService minIoService;

    @Override
    @Transactional
    public Result saveMusic(MusicDTO musicDTO, MultipartFile lyric, MultipartFile cover){

        if (!musicDTO.validate()){
            return Result.fail("音乐参数有错");
        }

        if (!minIoService.isAllowedFile(lyric, "music") || !minIoService.isAllowedFile(cover, "picture")){
            return Result.fail("文件格式不正确");
        }
        if (lyric.getSize() > SystemConstants.MAX_MUSIC_SIZE)
            return Result.fail("文件大小超出限制");
        if (cover.getSize() > SystemConstants.MAX_PICTURE_SIZE)
            return Result.fail("文件大小超出限制");

        String lyricName = minIoService.uploadFile(lyric);
        String coverName = minIoService.uploadFile(cover);

        if (lyricName == null || coverName == null){
            return Result.fail("文件上传失败");
        }

        long duration = AudioDurationUtil.getAudioDuration(lyric);
        if (duration == -1){
            return Result.fail("视频时长计算错误");
        }

        Music music = musicDTO.toEntity();
        music.setCoverUrl(coverName);
        music.setLyricUrl(lyricName);
        music.setDuration(duration);

        try {
            if(!this.save(music)){
                throw new RuntimeException("视频保存失败");  // 抛出异常触发回滚
            }
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.fail(e.getMessage());
        }

        return Result.ok();
    }

}
