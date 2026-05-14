package com.future.music.chunk;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Minio 大文件分片上传接口
 * 支持：秒传、断点续传、分片上传、文件合并
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Api(tags = "文件上传模块 - Minio分片上传")
public class MinioChunkUploadController {

    private final MinioChunkUploadService uploadService;

    /**
     * 秒传、断点续传校验接口
     * 前端上传前先调用，判断文件是否已存在（秒传）/ 已上传分片列表
     */
    @GetMapping("/check-breakpoint")
    @ApiOperation(value = "秒传/断点续传检查", notes = "根据文件MD5检查是否已上传，返回已上传分片序号")
    public BreakpointUploadVO checkBreakpoint(
            @ApiParam(value = "文件唯一MD5值", required = true)
            String fileMd5
    ) throws Exception{
        return uploadService.checkBreakpoint(fileMd5);
    }

    /**
     * 初始化分片上传
     * 生成上传ID、文件存储路径，返回给前端
     */
    @PostMapping("/init")
    @ApiOperation(value = "初始化分片上传", notes = "生成上传唯一标识uploadId和文件存储路径objectName")
    public InitUploadResponse initUpload(
            @ApiParam(value = "初始化上传请求参数", required = true)
            @RequestBody InitUploadRequest request
    ) {
        return uploadService.initUpload(request);
    }

    /**
     * 上传单个分片
     * 前端循环调用，分片上传；自动跳过已上传分片
     */
    @PostMapping("/chunk")
    @ApiOperation(value = "上传单个分片", notes = "支持断点续传，重复分片会自动跳过")
    public Map<String, Object> uploadChunk(
            @ApiParam(value = "上传唯一标识uploadId", required = true)
            @RequestParam("uploadId") String uploadId,

            @ApiParam(value = "分片序号（从0开始）", required = true)
            @RequestParam("chunkIndex") Integer chunkIndex,

            @ApiParam(value = "文件存储路径objectName", required = true)
            @RequestParam("objectName") String objectName,

            @ApiParam(value = "分片文件流", required = true)
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        uploadService.uploadChunk(uploadId, chunkIndex, objectName, file);

        return Map.of(
                "success", true,
                "chunkIndex", chunkIndex
        );
    }

    /**
     * 合并分片
     * 所有分片上传完成后调用，合并为完整文件并返回访问地址
     */
    @PostMapping("/complete")
    @ApiOperation(value = "合并所有分片", notes = "分片全部上传完成后调用，合并生成完整文件并清理临时分片")
    public Map<String, Object> completeUpload(
            @ApiParam(value = "合并文件请求参数", required = true)
            @RequestBody CompleteUploadRequest request
    ) throws Exception {

        String url = uploadService.completeUpload(request);

        return Map.of(
                "success", true,
                "url", url,
                "objectName", request.getObjectName()
        );
    }
}