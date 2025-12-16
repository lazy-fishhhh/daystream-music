package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.CollectionDTO;
import social.domain.dto.LikeDTO;
import social.domain.dto.PageDTO;
import social.domain.vo.CollectionVO;
import social.domain.vo.LikeMusicVO;
import social.domain.vo.LikeVO;

import java.util.List;

public interface UserBaseService {
    Result getUserInfo();
    Result<LikeVO> likeandUnlike(LikeDTO likeDTO);
    Result<CollectionVO> collectUnCollect(CollectionDTO collectionDTO);
    Result getLikeMusic(PageDTO pageDTO);
    Result getCollectAlbum(PageDTO pageDTO);

    Result getPlayList(PageDTO pageDTO);
    Result follow(Long userId);

    Result unFollow(Long userId);
    Result getFollowerList(PageDTO pageDTO);
    Result getFollowerCount();
    Result getFollowCount();
    Result getFollowListMusic(PageDTO pageDTO);
    Result getFollowListNormal(PageDTO pageDTO);
    Result getOtherUserInfo(Long userId);

}
