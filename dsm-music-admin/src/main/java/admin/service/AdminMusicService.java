package admin.service;

import admin.domain.dto.MusicAuditDTO;
import admin.domain.dto.MusicListDTO;
import admin.domain.vo.MusicVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

/**
 * 音乐作品审核服务接口
 */
public interface AdminMusicService {

    /**
     * 获取音乐作品审核列表
     * @param musicListDTO 音乐作品查询条件
     * @return 分页的音乐作品列表
     */
    Result<PageResp<MusicVO>> getMusicAuditList(MusicListDTO musicListDTO);

    /**
     * 查询单条音乐审核详情
     * @param musicId 音乐作品ID
     * @return 音乐作品详情
     */
    Result<MusicVO> getMusicAuditDetail(Long musicId);

    /**
     * 音乐作品审核
     *
     * @param musicAuditDTO 审核信息
     * @return 审核结果
     */
    Result<String> auditMusic(MusicAuditDTO musicAuditDTO);

    /**
     * 批量通过音乐作品审核
     * @param musicAuditDTO 批量审核信息
     * @return 审核结果
     */
    Result<String> batchPass(MusicAuditDTO musicAuditDTO);

    /**
     * 批量驳回音乐作品审核
     * @param musicAuditDTO 批量审核信息
     * @return 审核结果
     */
    Result<String> batchRefuse(MusicAuditDTO musicAuditDTO);
}
