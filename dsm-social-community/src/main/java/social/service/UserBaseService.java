package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.vo.CollectionVO;
import social.domain.vo.LikeVO;

@Service
public interface UserBaseService {
    Result<LikeVO> likeandUnlike(LikeDTO likeDTO);
    Result<CollectionVO> collectUnCollect(CollectionDTO collectionDTO);
}
