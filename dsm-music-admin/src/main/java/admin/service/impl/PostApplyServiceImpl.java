package admin.service.impl;

import admin.domain.dto.DeletedMusicianDTO;
import admin.domain.dto.HandleReportDTO;
import admin.domain.dto.PostApplyDTO;
import admin.domain.vo.AdminPostVO;
import admin.mapper.AdminPostMapper;
import admin.service.PostApplyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.social.CommunityPost;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class PostApplyServiceImpl implements PostApplyService {
    @Autowired
    private AdminPostMapper adminPostMapper;
    @Override
    public Result<PageResp<AdminPostVO>> getList(PostApplyDTO postApplyDTO) {
        //构建分页
        Page<AdminPostVO> page = new Page<>(postApplyDTO.getPageNum(), postApplyDTO.getPageSize());
        IPage<AdminPostVO> iPage = adminPostMapper.getList(page,postApplyDTO.getRole(), postApplyDTO.getKey(), postApplyDTO.getStatus());
        PageResp<AdminPostVO> pageResp = new PageResp<>();
        pageResp.setTotal(iPage.getTotal());
        pageResp.setRecords(iPage.getRecords());
        pageResp.setPages(iPage.getPages());
        pageResp.setCurrent(iPage.getCurrent());
        pageResp.setSize(iPage.getSize());
        return Result.ok(pageResp);
    }

    @Override
    public Result<String> passBatch(DeletedMusicianDTO ids) {
        try {
            List<String> idList = ids.getIds();
            //批量通过动态
            //因为类型的限制,所以传过来的都是一样的审批状态，只需要判断一个，如果不是未处理，则不能批量通过
            LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CommunityPost::getId, idList.get(0))
                    .in(CommunityPost::getStatus, 1,2);
            CommunityPost communityPost = adminPostMapper.selectOne(wrapper);
            if (communityPost != null) {
                return Result.fail("请选择未处理的审核进行操作");
            }
            //批量操作
            idList.stream()
                    .map(Long::valueOf)
                    .forEach(id -> {
                        adminPostMapper.updatePostStatus(id, 1);
                    });
            return Result.ok("操作成功");
        }catch (Exception e)
        {
            log.error("批量通过动态失败", e);
            return Result.fail("操作失败");
        }
    }

    @Override
    public Result<String> rejectBatch(DeletedMusicianDTO ids) {
        try {
            List<String> idList = ids.getIds();
            LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CommunityPost::getId, idList.get(0))
                    .in(CommunityPost::getStatus, 1,2);
            CommunityPost communityPost = adminPostMapper.selectOne(wrapper);
            if (communityPost != null) {
                return Result.fail("请选择未处理的审核进行操作");
            }
            //批量操作
            idList.stream()
                    .map(Long::valueOf)
                    .forEach(id -> {
                        adminPostMapper.updatePostStatus(id, 2);
                    });
            return Result.ok("操作成功");
        }catch (Exception e)
        {
            log.error("批量通过动态失败", e);
            return Result.fail("操作失败");
        }
    }

    @Override
    public Result<String> handle(HandleReportDTO handleDto) {
        try{
            Long id=Long.parseLong(handleDto.getId());
            Integer status=handleDto.getHandleResult();
            adminPostMapper.updatePostStatus(id,status);
            return Result.ok("操作成功");
        }catch (Exception e)
        {
            log.error("处理动态失败", e);
            return Result.fail("操作失败");
        }

    }
}
