//package com.future.music.controller;
//
//import com.future.music.domain.dto.PosterGenerateDTO;
//import com.future.music.domain.vo.PosterVO;
//import com.future.music.service.IPosterService;
//import entity.pojo.util.Result;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/poster")
//@Api(tags = "海报生成")
//public class PosterController {
//
//    @Autowired
//    private IPosterService posterService;
//
//    @ApiOperation(value = "生成歌曲分享海报")
//    @PostMapping("/generate")
//    public Result<PosterVO> generatePoster(@RequestBody PosterGenerateDTO dto) {
//        return posterService.generatePoster(dto);
//    }
//}