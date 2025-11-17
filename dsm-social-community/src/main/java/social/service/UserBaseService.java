package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.LikeDTO;
@Service
public interface UserBaseService {
    Result likeandUnlike(LikeDTO likeDTO);
}
