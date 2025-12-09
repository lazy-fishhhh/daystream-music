package com.future.music.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.mapper.PlaylistMusicMapper;
import com.future.music.service.IPlaylistMusicService;
import entity.pojo.music.PlaylistMusic;
import org.springframework.stereotype.Service;

@Service
public class PlaylistMusicServiceImpl extends ServiceImpl<PlaylistMusicMapper, PlaylistMusic> implements IPlaylistMusicService {
}
