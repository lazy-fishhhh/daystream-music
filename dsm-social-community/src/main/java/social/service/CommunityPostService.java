package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.CommunityDTO;

@Service
public interface CommunityPostService {
    Result releasePost(CommunityDTO communityDTO);
}
