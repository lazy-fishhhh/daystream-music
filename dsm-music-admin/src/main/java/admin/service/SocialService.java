package admin.service;

import admin.domain.dto.AdminPostDTO;
import admin.domain.dto.DeletedMusicianDTO;
import admin.domain.dto.UpdatePostDTO;
import admin.domain.vo.AdminPostVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;

public interface SocialService {
    Result<String> deletedCommunity(String id);
    Result<PageResp<AdminPostVO>> communityList(AdminPostDTO adminPostDTO);

    Result<String> deletedCommunityBatch(DeletedMusicianDTO deletedMusicianDTO);

    Result<String> banPost(String id);

    Result<AdminPostVO> detailPost(String id);

    Result<String> updatePost(UpdatePostDTO updatePostDTO);
}
