package com.future.music.service.impl;


import com.future.music.mapper.MusicianMapper;
import com.future.music.service.IMusicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MusicianServiceImpl implements IMusicianService {
    private  final MusicianMapper musicianMapper;

}
