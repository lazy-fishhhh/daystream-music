package com.future.music.controller;


import com.future.music.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class UserController {
    //构造方法注入
    private final IUserService userService;





}
