package com.future.music.controller;

import com.future.music.service.IFileService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Api("MV分片上传")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    
    @Resource
    private IFileService fileService;
    /**
     * 检验分片文件是否已经上传到MinIO
     *
     * @param fileMd5  整体文件md5值
     * @param chunk    当前上传分片在所有分片文件中索引位置
     * @param chunkMd5 分片文件的md5值
     * @return 是否已经上传
     */
    @PostMapping("/check")
    public Result<Boolean> check(String fileMd5, String chunk, String chunkMd5) {
        return Result.ok(fileService.check(fileMd5, chunk, chunkMd5));
    }

    /**
     * 分片上传接口
     *
     * @param request
     * @param multipartFile
     * @return
     * @throws IOException
     */
    @PostMapping("/upload")
    public Result<String> upload(HttpServletRequest request, MultipartFile multipartFile) {
        try {
            return Result.ok(fileService.upload(request, multipartFile));
        } catch (IOException e) {
            log.error("分片上传失败", e);
            return Result.fail("分片上传失败：" + e.getMessage());
        }
    }

    /**
     * 合并分片文件接口
     *
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping("/merge")
    public Result<String> merge(HttpServletRequest request) {
        try {
            return Result.ok(fileService.merge(request));
        } catch (IOException e) {
            log.error("合并分片失败", e);
            return Result.fail("合并分片失败：" + e.getMessage());
        }
    }

//    @GetMapping("/download")
//    public Result<String> download(String fileName, HttpServletResponse response) {
//        try {
//            fileService.download(fileName, response);
//            return Result.ok("下载成功");
//        } catch (IOException e) {
//            log.error("文件下载失败", e);
//            return Result.fail("文件下载失败：" + e.getMessage());
//        }
//    }


}
