package admin.service;

import admin.domain.dto.DeletedMusicianDTO;
import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.PostApplyDTO;
import admin.domain.vo.AdminPostVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

public interface PostApplyService{
    Result<PageResp<AdminPostVO>> getList(PostApplyDTO postApplyDTO);

    Result<String> passBatch(DeletedMusicianDTO ids);

    Result<String> rejectBatch(DeletedMusicianDTO ids);

    Result<String> handle(HandleReportDTO handleDto);
}
