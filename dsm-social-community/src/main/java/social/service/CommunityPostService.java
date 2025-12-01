package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.CommunityDTO;
import social.domain.dto.CommunityVOListDTO;

@Service
public interface CommunityPostService {
    Result releasePost(CommunityDTO communityDTO);

    Result deletePost(String postId);
    Result getPost(CommunityVOListDTO communityVOListDTO);
    Result getDetail(String postId);
}
