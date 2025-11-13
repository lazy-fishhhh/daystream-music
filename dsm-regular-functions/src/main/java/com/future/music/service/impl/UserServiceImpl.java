package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.future.login.domain.LoginUserDetails;
import com.future.music.mapper.PlayListMapper;
import com.future.music.service.IUserService;
import entity.dto.PlayListDTO;
import entity.pojo.music.Playlist;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@Service
public class UserServiceImpl  implements IUserService {

    private final PlayListMapper playListMapper;
    @Override
    public void createPlayList(PlayListDTO playListDTO) {

        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Playlist playList = new Playlist();
        playList.setUserId(userId);
        playList.setName(playListDTO.getName());
        playList.setCover(playListDTO.getCover());
        playList.setDescription(playListDTO.getDescription());
        playList.setIsPublic(playListDTO.getIsPublic());
        playList.setCreatedTime(LocalDateTime.now());
        playList.setUpdatedTime(LocalDateTime.now());

        playListMapper.insert(playList);

    }

    @Override
    public void updatePlayList(PlayListDTO dto) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getId,dto.getId());
        Playlist playlist1 = playListMapper.selectOne(lqw);
        if (playlist1 == null){
            throw new RuntimeException("歌单不存在");
        }

        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Long playListUserId = playlist1.getUserId();

        if (!playListUserId.equals(userId)) {
            throw new RuntimeException("您没有权限修改此歌单");
        }

    
        // 创建 PlayList 对象并设置 ID 属性
        Playlist playlist = new Playlist();
        //updateById是通过id来更新的，所以必须设置id
        playlist.setId(dto.getId());
        playlist.setName(dto.getName());
        playlist.setCover(dto.getCover());
        playlist.setDescription(dto.getDescription());
        playlist.setIsPublic(dto.getIsPublic());
        playlist.setUpdatedTime(LocalDateTime.now());

        // 使用 MyBatis-Plus 的 updateById 方法
        playListMapper.updateById(playlist);
    }

    @Override
    public PageResp<Playlist> getPlayList(PageReq pageReq) {
        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId  = loginUser.getUser().getId();
        // 泛型 <PlayList> 明确指定了该查询条件是针对 PlayList 实体类的
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getUserId, userId);

        // 1. 构建 MyBatis-Plus 的 Page 对象
        Page<Playlist> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        // 2. 执行分页查询
        IPage<Playlist> playListPage = playListMapper.selectPage(page, lqw);
        // 3. 构建统一的响应对象
        PageResp<Playlist> pageResp = new PageResp<>();
        pageResp.setRecords(playListPage.getRecords());
        pageResp.setTotal(playListPage.getTotal());
        pageResp.setSize(playListPage.getSize());
        pageResp.setCurrent(playListPage.getCurrent());
        pageResp.setPages(playListPage.getPages());
        return pageResp;
    }
}