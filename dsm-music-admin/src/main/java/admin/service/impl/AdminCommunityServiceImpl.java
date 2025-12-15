package admin.service.impl;

import admin.domain.dto.AdminPostDTO;
import admin.domain.dto.DeletedMusicianDTO;
import admin.domain.dto.UpdatePostDTO;
import admin.domain.vo.AdminPostVO;
import admin.mapper.AdminPostMapper;
import admin.service.SocialService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.social.CommunityPost;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@Slf4j
public class AdminCommunityServiceImpl implements SocialService {
    @Resource
    private AdminPostMapper adminPostMapper;
    //删除动态
    @Override
    public Result<String> deletedCommunity(String id){
        Long communityId = Long.parseLong(id);
        //查看动态是否删除
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPost::getId,communityId)
                .eq(CommunityPost::getIsDeleted,0)
                .eq(CommunityPost::getStatus,1);
        CommunityPost communityPost = adminPostMapper.selectOne(queryWrapper);
        if(communityPost == null){
            return Result.fail("动态不存在");
        }
        //删除动态
        UpdateWrapper<CommunityPost> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",communityId)
                .set("is_deleted",1)
                        .set("delete_time", LocalDateTime.now());
        adminPostMapper.update(null,updateWrapper);
        return Result.ok("删除成功");
    }

    @Override
    public Result<PageResp<AdminPostVO>> communityList(AdminPostDTO adminPostDTO) {
        //构建分页
        Page<AdminPostVO> page = new Page<>(adminPostDTO.getPageNum(),adminPostDTO.getPageSize());
        IPage <AdminPostVO> communityPostIPage =adminPostMapper.communityList(page,adminPostDTO.getRole(),adminPostDTO.getKey());
        PageResp<AdminPostVO> pageResp = new PageResp<>();
        pageResp.setTotal(communityPostIPage.getTotal());
        pageResp.setCurrent(communityPostIPage.getCurrent());
        pageResp.setRecords(communityPostIPage.getRecords());
        pageResp.setPages(communityPostIPage.getPages());
        pageResp.setSize(communityPostIPage.getSize());
        return Result.ok(pageResp);

    }

    @Override
    public Result<String> deletedCommunityBatch(DeletedMusicianDTO deletedMusicianDTO) {
        try {
            List<String> ids = deletedMusicianDTO.getMusicianIds();
            ids.stream()
                    .map(Long::valueOf)
                    .forEach(id -> {
                      adminPostMapper.deletedPost(id,1);
                    });
            return Result.ok("批量删除成功");
        }catch (Exception e)
        {
            log.error("批量删除音乐人失败", e);
            return Result.fail("批量删除失败");
        }

    }

    @Override
    public Result<String> banPost(String id) {
        //判断动态是否存在
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPost::getId,id)
                .in(CommunityPost::getIsDeleted,0,2)
                .eq(CommunityPost::getStatus,1);
        CommunityPost communityPost = adminPostMapper.selectOne(queryWrapper);
        if(communityPost == null){
            return Result.fail("动态不存在");
        }
        //如果动态是正常状态，封禁
        if(communityPost.getStatus() == 0){
            UpdateWrapper<CommunityPost> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id",id)
                    .set("is_deleted",2);
            adminPostMapper.update(null,updateWrapper);
            return Result.ok("封禁成功");
        }
        //如果动态是封禁状态，解封
        if(communityPost.getStatus() == 2){
            UpdateWrapper<CommunityPost> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id",id)
                    .set("is_deleted",0);
            adminPostMapper.update(null,updateWrapper);
            return Result.ok("解封成功");
        }
        return Result.fail("操作失败");
    }

    @Override
    public Result<AdminPostVO> detailPost(String id) {
        Long postId=Long.parseLong(id);
        //判断动态是否存在
        LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPost::getId,postId)
                .in(CommunityPost::getIsDeleted,0,2)
                .eq(CommunityPost::getStatus,1);
                CommunityPost communityPost = adminPostMapper.selectOne(queryWrapper);
                if(communityPost == null){
                    return Result.fail("动态不存在");
                }
        //存在的话就获取详情
        AdminPostVO adminPostVO = adminPostMapper.detailPost(postId);
        return Result.ok(adminPostVO);
    }

    @Override
    public Result<String> updatePost(UpdatePostDTO updatePostDTO) {
        try {
            Long postId = Long.parseLong(updatePostDTO.getId());
            //判断动态是否存在
            LambdaQueryWrapper<CommunityPost> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CommunityPost::getId, postId)
                    .in(CommunityPost::getIsDeleted, 0, 2)
                    .eq(CommunityPost::getStatus, 1);
            CommunityPost communityPost = adminPostMapper.selectOne(queryWrapper);
            if (communityPost == null) {
                return Result.fail("动态不存在");
            }
            adminPostMapper.updatePost(postId, updatePostDTO.getStatus(), updatePostDTO.getTitle(), updatePostDTO.getTitle());
            return Result.ok("修改成功");
        }catch (Exception e) {
            log.error("修改失败", e);
            return Result.fail("修改失败");
        }
    }
}
