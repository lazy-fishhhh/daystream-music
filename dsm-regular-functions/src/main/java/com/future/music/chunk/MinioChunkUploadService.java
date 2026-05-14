package com.future.music.chunk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.future.music.mapper.FileChunkMapper;
import com.future.music.mapper.FileInfoMapper;
import com.future.music.chunk.*;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Minio 大文件分片上传服务
 * 支持：秒传、断点续传、分片合并、临时文件清理
 */
@Service
@RequiredArgsConstructor
public class MinioChunkUploadService {

    // Minio 客户端
    private final MinioClient minioClient;
    // 文件信息 Mapper
    private final FileInfoMapper fileInfoMapper;
    // 文件分片 Mapper
    private final FileChunkMapper fileChunkMapper;

    // 从配置文件读取 Minio 存储桶名称
    @Value("${minio.bucket-name}")
    private String bucket;

    /**
     * 秒传 + 断点续传：查询已上传分片
     * 前端上传前先调用，判断文件是否已存在（秒传）/ 哪些分片已上传（断点续传）
     * @param fileMd5 文件唯一标识MD5
     * @return 断点续传VO（是否存在、已上传分片列表）
     */
    public BreakpointUploadVO checkBreakpoint(String fileMd5) throws Exception {
        BreakpointUploadVO vo = new BreakpointUploadVO();

        // 1. 秒传判断：查询文件是否已完整上传完成（上传状态=1）
        LambdaQueryWrapper<FileInfo> infoWrapper = Wrappers.lambdaQuery();
        infoWrapper.eq(FileInfo::getFileMd5, fileMd5)
                .eq(FileInfo::getUploadStatus, 1);
        FileInfo info = fileInfoMapper.selectOne(infoWrapper);

        // 文件已存在，直接返回文件地址，实现秒传
        if (info != null) {
            vo.setExist(true);
            vo.setFileUrl(getPresignedUrl(info.getObjectName()));
            return vo;
        }

        // 2. 断点续传：查询该文件已上传成功的分片序号
        LambdaQueryWrapper<FileChunk> chunkWrapper = Wrappers.lambdaQuery();
        chunkWrapper.eq(FileChunk::getUploadId, fileMd5);
        List<FileChunk> chunks = fileChunkMapper.selectList(chunkWrapper);

        if (chunks != null && !chunks.isEmpty()) {
            // 把已上传分片序号存入集合，返回给前端跳过上传
            Set<Integer> uploadedChunks = new HashSet<>();
            for (FileChunk chunk : chunks) {
                uploadedChunks.add(chunk.getChunkIndex());
                vo.setUploadId(fileMd5);
                vo.setObjectName(chunk.getObjectName());
            }

            vo.setUploadedChunks(uploadedChunks);
        }
        vo.setExist(false);
        return vo;
    }


    /**
     * 初始化分片上传
     * 生成唯一上传ID、文件存储路径（objectName）
     * @param request 初始化请求参数（文件名、MD5等）
     * @return 上传ID + 文件存储路径
     */
    public InitUploadResponse initUpload(InitUploadRequest request) {
        // 直接用文件MD5作为上传唯一标识
        String uploadId = request.getFileMd5();

        // 获取文件后缀名
        String suffix = getSuffix(request.getFilename());

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = "files/"
                + LocalDate.now()
                + "/"
                + uuid  // 这里用 UUID，不要用 MD5
                + suffix;

        return new InitUploadResponse(uploadId, objectName);
    }

    /**
     * 上传单个分片
     * 前端循环调用，分片上传；自动跳过已上传分片
     * @param uploadId 上传唯一标识（文件MD5）
     * @param chunkIndex 当前分片序号
     * @param file 分片文件流
     */
    public void uploadChunk(
            String uploadId,
            Integer chunkIndex,
            String objectName,
            MultipartFile file
    ) throws Exception {

        // 参数合法性校验
        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId不能为空");
        }
        if (chunkIndex == null || chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex非法");
        }

        // 1. 先查数据库：该分片是否已经上传过
        LambdaQueryWrapper<FileChunk> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FileChunk::getUploadId, uploadId)
                .eq(FileChunk::getChunkIndex, chunkIndex);

        FileChunk existChunk = fileChunkMapper.selectOne(wrapper);

        // 2. 已存在 → 直接返回，不执行上传、不重复存储
        if (existChunk != null) {
            return;
        }

        // 生成分片在 Minio 中的临时存储路径
        String chunkObjectName = getChunkObjectName(uploadId, chunkIndex);

        // 3. 上传分片到 Minio
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(chunkObjectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // 4. 记录分片信息（断点续传核心依据）
        FileChunk chunk = new FileChunk();
        chunk.setUploadId(uploadId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setObjectName(objectName);
        chunk.setCreateTime(LocalDateTime.now());
        fileChunkMapper.insert(chunk);
    }

    /**
     * 合并分片
     * 所有分片上传完成后调用，合并为完整文件，清理临时分片
     * @param request 合并请求参数
     * @return 完整文件的访问地址
     */
    public String completeUpload(CompleteUploadRequest request) throws Exception {
        String uploadId = request.getUploadId();
        String objectName = request.getObjectName();
        Integer totalChunks = request.getTotalChunks();

        // 参数校验
        if (totalChunks == null || totalChunks <= 0) {
            throw new IllegalArgumentException("totalChunks非法");
        }

        // 组装所有分片源，用于合并
        List<ComposeSource> sources = new ArrayList<>();

        for (int i = 0; i < totalChunks; i++) {
            String chunkObjectName = getChunkObjectName(uploadId, i);

            // 检查分片是否存在，不存在会抛出异常
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(chunkObjectName)
                            .build()
            );

            // 添加到合并源列表
            sources.add(
                    ComposeSource.builder()
                            .bucket(bucket)
                            .object(chunkObjectName)
                            .build()
            );
        }

        // 执行 Minio 分片合并
        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .sources(sources)
                        .build()
        );

        // 删除 Minio 中的临时分片文件
        removeChunks(uploadId, totalChunks);

        // 4. 删除数据库中的分片记录
        LambdaQueryWrapper<FileChunk> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FileChunk::getUploadId, uploadId);
        fileChunkMapper.delete(wrapper);

        // 5. 记录完整文件信息，标记上传完成
        FileInfo info = new FileInfo();
        info.setFileMd5(uploadId);
        info.setObjectName(objectName);
        info.setUploadStatus(1);
        fileInfoMapper.insert(info);

        // 返回带签名的文件访问地址
        return getPresignedUrl(objectName);
    }

    /**
     * 删除 Minio 中的临时分片文件
     * @param uploadId 上传唯一标识
     * @param totalChunks 总分片数
     */
    private void removeChunks(String uploadId, int totalChunks) throws Exception {
        List<DeleteObject> objects = new LinkedList<>();

        // 组装所有需要删除的分片
        for (int i = 0; i < totalChunks; i++) {
            objects.add(new DeleteObject(getChunkObjectName(uploadId, i)));
        }

        // 批量删除
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucket)
                        .objects(objects)
                        .build()
        );

        // 必须遍历结果，Minio才会真正执行删除
        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
            System.err.println("删除分片失败: " + error.objectName() + ", " + error.message());
        }
    }

    /**
     * 获取 Minio 文件的预签名访问地址（有时效性，安全）
     * @param objectName 文件路径
     * @return 可直接访问的URL
     */
    private String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(60 * 60) // 有效期1小时
                        .build()
        );
    }

    /**
     * 生成分片在 Minio 中的临时对象名
     * @param uploadId 上传唯一标识
     * @param chunkIndex 分片序号
     * @return 临时路径
     */
    private String getChunkObjectName(String uploadId, Integer chunkIndex) {
        return "temp/chunks/" + uploadId + "/" + chunkIndex;
    }

    /**
     * 安全获取文件后缀名
     * @param filename 原始文件名
     * @return 后缀（如 .mp4）
     */
    private String getSuffix(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        // URL编码防止中文/特殊字符问题
        String safeName = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        int index = safeName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }

        return safeName.substring(index);
    }
}