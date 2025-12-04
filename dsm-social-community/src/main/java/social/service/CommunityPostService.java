package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.CommunityDTO;
import social.domain.dto.CommunityVOListDTO;
import social.domain.dto.MyPostDTO;
import social.domain.dto.UpdatePostDTO;

@Service
public interface CommunityPostService {
    Result releasePost(CommunityDTO communityDTO);

    Result deletePost(String postId);
    Result getPost(CommunityVOListDTO communityVOListDTO);
    Result getDetail(String postId);
    Result updatePost(UpdatePostDTO updatePostDTO);

    Result selectMypost(MyPostDTO myPostDTO);
}
