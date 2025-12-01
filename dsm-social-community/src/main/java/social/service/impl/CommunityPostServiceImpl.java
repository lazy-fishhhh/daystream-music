package social.service.impl;

import com.future.login.util.SecurityUtil;
import entity.pojo.social.CommunityPost;
import entity.pojo.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.saml2.Saml2SecurityMarker;
import org.springframework.stereotype.Service;
import social.domain.dto.CommunityDTO;
import social.mapper.CommunityPostMapper;
import social.service.CommunityPostService;

@Service
public class CommunityPostServiceImpl implements CommunityPostService {
    @Autowired
    private CommunityPostMapper communityPostMapper;
    //发布社区动态
    @Override
    public Result releasePost(CommunityDTO communityDTO) {
        //获取当前用户的 id
        Long currentUserId = SecurityUtil.getCurrentUserId();
        //创建动态
        CommunityPost communityPost=new CommunityPost();
        communityPost.setUserId(currentUserId);
        communityPost.setTitle(communityDTO.getTitle());
        communityPost.setContent(communityDTO.getContent());
        //保存动态
        int insert = communityPostMapper.insert(communityPost);
        if(insert>0){
            return Result.ok("发布成功");
        }else
            return Result.fail("发布失败");
    }
}
