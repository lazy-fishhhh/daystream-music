package com.future.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import entity.pojo.music.MusicMv;
import com.future.music.domain.dto.MusicMvDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.dto.FileUploadDTO;
import entity.vo.MusicMvVO;

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
     * 更新MV播放量
     * @param mvId MV ID
     * @return 更新结果
     */
    Result<Boolean> updatePlayCount(Long mvId);
}
