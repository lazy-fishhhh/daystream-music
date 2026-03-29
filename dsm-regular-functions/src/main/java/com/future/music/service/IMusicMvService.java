package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import entity.pojo.music.MusicMv;
import com.future.music.domain.dto.MusicMvDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.dto.FileUploadDTO;
import entity.vo.MusicMvVO;

import java.util.List;

/**
 * MV相关的Service接口
 */
public interface IMusicMvService extends IService<MusicMv> {

    /**
     * 上传MV文件
     * @param fileUploadDTO 文件上传参数
     * @return 上传结果
     */
    Result<String> uploadMv(FileUploadDTO fileUploadDTO);

    /**
     * 合并MV分片文件
     * @param fileUploadDTO 文件上传参数
     * @return 合并结果
     */
    Result<String> mergeMvChunks(FileUploadDTO fileUploadDTO);

    /**
     * 创建MV
     * @param musicMvDTO MV相关参数
     * @return 创建结果
     */
    Result<Long> createMv(MusicMvDTO musicMvDTO);

    /**
     * 更新MV信息
     * @param mvId MV ID
     * @param musicMvDTO MV相关参数
     * @return 更新结果
     */
    Result<Boolean> updateMv(Long mvId, MusicMvDTO musicMvDTO);

    /**
     * 删除MV
     * @param mvId MV ID
     * @return 删除结果
     */
    Result<Boolean> deleteMv(Long mvId);

    /**
     * 根据MV ID查询MV详情
     *
     * @param mvId MV ID
     * @return MV详情
     */
    Result<MusicMvVO> getMvDetail(Long mvId);

    /**
     * 根据音乐人ID分页查询其所有MV作品
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param musicianId 音乐人ID
     * @return 分页的MV列表
     */
    Result<IPage<MusicMvVO>> getMvsByMusicianId(Long musicianId, Integer pageNum, Integer pageSize);

    /**
     * 根据音乐ID查询对应的MV
     * @param musicId 音乐ID
     * @return MV详情
     */
    Result<MusicMvVO> getMvByMusicId(Long musicId);

    /**
     * 分页查询热门MV
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页的热门MV列表
     */
    Result<PageResp<MusicMvVO>> getHotMvs(Integer pageNum, Integer pageSize);

    /**
     * 分页查询最新MV
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页的最新MV列表
     */
    Result<PageResp<MusicMvVO>> getNewMvs(Integer pageNum, Integer pageSize);

    /**
     * 分页查询VIP专属MV
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页的VIP专属MV列表
     */
    Result<PageResp<MusicMvVO>> getVipMvs(Integer pageNum, Integer pageSize);

    /**
     * 根据时间、收藏量筛选MV
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param minCollectCount 最小收藏量（可选）
     * @param maxCollectCount 最大收藏量（可选）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的MV列表
     */
    Result<PageResp<MusicMvVO>> filterMvs(String startTime, String endTime, 
                                          Long minCollectCount, Long maxCollectCount, 
                                          Integer pageNum, Integer pageSize);

    /**
     * 将歌曲添加到指定MV
     * @param mvId MV ID
     * @param musicId 歌曲ID
     * @return 添加结果
     */
    Result<Boolean> addMusicToMv(Long mvId, Long musicId);

    /**
     * 将歌曲从其对应的MV中移除
     * @param musicId 歌曲ID
     * @return 移除结果
     */
    Result<Boolean> removeMusicFromMv(Long musicId);

    /**
     * 查询未指定音乐的MV列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的MV列表
     */
    Result<PageResp<MusicMvVO>> getMvsWithoutMusic(Integer pageNum, Integer pageSize);

    /**
     * 查询未指定MV的音乐列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的音乐列表
     */
    Result<PageResp<entity.pojo.music.Music>> getMusicsWithoutMv(Integer pageNum, Integer pageSize);

    /**
     * 分页查询我的MV（按创建时间降序）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的MV列表
     */
    Result<PageResp<MusicMvVO>> getMyMvsByCreateTime(Integer pageNum, Integer pageSize);

    /**
     * 分页查询我的MV（按收藏量降序）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的MV列表
     */
    Result<PageResp<MusicMvVO>> getMyMvsByCollectCount(Integer pageNum, Integer pageSize);

    /**
     * 获取MV收藏者分页列表
     * @param mvId MV ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页的用户列表
     */
    Result<PageResp<entity.pojo.info.User>> getMvCollectors(Long mvId, Integer pageNum, Integer pageSize);

}
