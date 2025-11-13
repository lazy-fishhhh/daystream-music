package com.future.music.controller;



import com.future.music.service.IMusicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;


@RequiredArgsConstructor
@Controller
public class MusicianController {
    public final IMusicianService musicianService;



}
