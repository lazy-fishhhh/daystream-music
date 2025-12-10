package com.future.player.service;


import com.future.player.entity.dto.AlbumDTO;
import com.future.player.entity.vo.AlbumVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

import java.util.List;

public interface IAlbumService {
    /**
     * 创建专辑
     * @param albumDTO 专辑信息
     * @return 操作结果
     */
    Result<AlbumVO> createAlbum(AlbumDTO albumDTO);

    /**
     * 修改专辑
     * @param albumDTO 专辑信息
     * @return 操作结果
     */
    Result<Void> updateAlbum(AlbumDTO albumDTO);

    /**
     * 删除专辑
     * @param albumId 专辑ID
     * @return 操作结果
     */
    Result<Void> deleteAlbum(Long albumId);

    /**
     * 分页查询我的所有专辑
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Result<PageResp<AlbumVO>> queryMyAlbum(int pageNum, int pageSize);

    /**
     * 分页查询指定人的公开专辑
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Result<PageResp<AlbumVO>> queryOtherAlbum(Long userId, int pageNum, int pageSize);

    /**
     * 分页查询所有公开专辑
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Result<PageResp<AlbumVO>> queryAllAlbum(String key, int pageNum, int pageSize);

    /**
     * 获取专辑详细内容
     * @param albumId 专辑ID
     * @return 专辑详情
     */
    Result<AlbumVO> getAlbumDetail(Long albumId);

    Result<PageResp<AlbumVO>> queryPublicAlbum(int pageNum, int pageSize);

    /**
     * 关联歌曲到专辑
     * @param albumId 专辑ID
     * @param songIds 歌曲ID列表
     * @return 操作结果
     */
    Result<String> bindSongsToAlbum(Long albumId, List<Long> songIds);
}