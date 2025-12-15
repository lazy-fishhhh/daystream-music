package admin.service;

import admin.domain.dto.MvAuditDTO;
import admin.domain.dto.MvListDTO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import entity.vo.MusicMvVO;

/**
 * MV审核服务接口
 */
public interface AdminMusicMvService {

    /**
     * 获取MV审核列表
     * @param mvListDTO MV查询条件
     * @return 分页的MV列表
     */
    Result<PageResp<MusicMvVO>> getMvAuditList(MvListDTO mvListDTO);

    /**
     * 查询单条MV审核详情
     * @param mvId MV ID
     * @return MV详情
     */
    Result<MusicMvVO> getMvAuditDetail(Long mvId);

    /**
     * MV审核
     * @param mvAuditDTO 审核信息
     * @return 审核结果
     */
    Result<String> auditMv(MvAuditDTO mvAuditDTO);

    /**
     * 批量通过MV审核
     * @param mvAuditDTO 批量审核信息
     * @return 审核结果
     */
    Result<String> batchPassMv(MvAuditDTO mvAuditDTO);

    /**
     * 批量驳回MV审核
     * @param mvAuditDTO 批量审核信息
     * @return 审核结果
     */
    Result<String> batchRefuseMv(MvAuditDTO mvAuditDTO);
}