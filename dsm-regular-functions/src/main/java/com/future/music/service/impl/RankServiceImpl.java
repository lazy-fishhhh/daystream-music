package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.music.domain.vo.MusicVO;
import com.future.music.domain.vo.MusicianVO;
import com.future.music.domain.vo.RankHomeVO;
import com.future.music.domain.vo.RankVO;
import com.future.music.mapper.MusicianMapper;
import com.future.music.mapper.PlaylistMapper;
import com.future.music.mapper.PlaylistMusicMapper;
import com.future.music.service.IMusicService;
import com.future.music.service.IRankService;
import constants.RankConstants;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.music.Playlist;
import entity.pojo.music.PlaylistMusic;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankServiceImpl implements IRankService {

    @Resource
    private IMusicService musicService;
    @Resource
    private PlaylistMusicMapper playlistMusicMapper;
    @Resource
    private PlaylistMapper playlistMapper;
    @Resource
    private MusicianMapper musicianMapper;

    // 进入榜单
    @Override
    public Result<RankHomeVO> getRankHome(){
        Page<Music> musicPage = new Page<>(1, 6);

        // 查询热歌榜
        RankVO hotRank = new RankVO();
        Playlist hotPlaylist = playlistMapper.selectById(RankConstants.HOT_MUSIC_RANK_ID);
        BeanUtils.copyProperties(hotPlaylist,hotRank);
        // 查询热歌榜中的音乐
        IPage<Music> hotMusicIPage = playlistMusicMapper.selectMusicsByPlaylistId(musicPage, hotPlaylist.getId());
        for(Music music:hotMusicIPage.getRecords()){
            MusicVO musicVO = musicService.getMusicVO(music.getId());
            hotRank.getMusicList().add(musicVO);
        }

        // 查询新歌榜
        RankVO newRank = new RankVO();
        Playlist newPlaylist = playlistMapper.selectById(RankConstants.NEW_MUSIC_RANK_ID);
        BeanUtils.copyProperties(newPlaylist,newRank);
        // 查询新歌榜中的音乐
        IPage<Music> newMusicIPage = playlistMusicMapper.selectMusicsByPlaylistId(musicPage, newPlaylist.getId());
        for(Music music:newMusicIPage.getRecords()){
            MusicVO musicVO = musicService.getMusicVO(music.getId());
            newRank.getMusicList().add(musicVO);
        }

        // 查询VIP热歌榜
        RankVO vipHotRank = new RankVO();
        Playlist vipHotPlaylist = playlistMapper.selectById(RankConstants.VIP_HOT_MUSIC_RANK_ID);
        BeanUtils.copyProperties(vipHotPlaylist,vipHotRank);
        // 查询VIP热歌榜中的音乐
        IPage<Music> vipHotMusicIPage = playlistMusicMapper.selectMusicsByPlaylistId(musicPage, vipHotPlaylist.getId());
        for(Music music:vipHotMusicIPage.getRecords()){
            MusicVO musicVO = musicService.getMusicVO(music.getId());
            vipHotRank.getMusicList().add(musicVO);
        }

        // 查询新歌榜
        RankVO vipNewRank = new RankVO();
        Playlist vipNewPlaylist = playlistMapper.selectById(RankConstants.VIP_NEW_MUSIC_RANK_ID);
        BeanUtils.copyProperties(vipNewPlaylist,vipNewRank);
        // 查询VIP新歌榜中的音乐
        IPage<Music> vipNewMusicIPage = playlistMusicMapper.selectMusicsByPlaylistId(musicPage, vipNewPlaylist.getId());
        for(Music music:vipNewMusicIPage.getRecords()){
            MusicVO musicVO = musicService.getMusicVO(music.getId());
            vipNewRank.getMusicList().add(musicVO);
        }

        // 查询音乐人榜
        List<MusicianVO> musicianVoList = new ArrayList<>();
        List<Musician> musicianList = musicianMapper.selectList(new LambdaQueryWrapper<Musician>()
                .orderByDesc(Musician::getTotalFans)
                .last("limit 10"));
        for(Musician musician:musicianList){
            MusicianVO musicianVO = new MusicianVO();
            BeanUtils.copyProperties(musician,musicianVO);

            // 查询音乐人热门音乐
            List<String> hotMusics = musicService.listObjs(new LambdaQueryWrapper<Music>()
                            .select(Music::getMusicName)
                            .orderByDesc(Music::getPlayCount)
                    .last("limit 3"),
            r -> (String) r
            );
            musicianVO.setHotMusics(hotMusics);

            musicianVoList.add(musicianVO);
        }

        // 构建榜单首页VO
        Map<String, RankVO> rankVOMap = new HashMap<>();
        rankVOMap.put("hotRank", hotRank);
        rankVOMap.put("newRank", newRank);
        rankVOMap.put("vipHotRank", vipHotRank);
        rankVOMap.put("vipNewRank", vipNewRank);
        RankHomeVO rankHomeVO = new RankHomeVO();
        rankHomeVO.setRankVOMap(rankVOMap);
        rankHomeVO.setMusicianList(musicianVoList);

        return Result.ok(rankHomeVO);
    }

    /**
     * 更新热歌榜
     * 每日凌晨1点0分0秒执行（cron表达式：秒 分 时 日 月 周 年（可选））
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateHotMusicRank() {
        // 从数据库中根据播放量从高到低排序查询出前100首音乐的id
        List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                        .select(Music::getId) // 仅查询ID字段，减少数据量
                        .orderByDesc(Music::getPlayCount)
                        .last("limit 100"),
                r -> ((Number) r).longValue()
        );
        if (playlistMapper.selectById(RankConstants.HOT_MUSIC_RANK_ID) == null) {
            // 如果热歌榜不存在，则创建一个新的热歌榜
            Playlist playlist = new Playlist();
            playlist.setId(RankConstants.HOT_MUSIC_RANK_ID);
            playlist.setName("热歌榜");
            playlist.setUserId(15L);
            playlist.setDescription("热歌榜：所有线上歌曲官方TOP排行榜，每日更新。");
            playlist.setIsPublic(1);
            playlist.setMusicCount((long) musicIds.size());
            playlist.setPlayCount(0L);
            playlist.setCreatedTime(LocalDateTime.now());
            playlist.setUpdatedTime(LocalDateTime.now());
            playlist.setCollectCount(0L);
            playlist.setCommentCount(0L);

            playlistMapper.insert(playlist);

            addMusicToRank(RankConstants.HOT_MUSIC_RANK_ID, musicIds);

            return;
        }
        // 清空热歌榜中的所有音乐
        playlistMusicMapper.delete(new LambdaQueryWrapper<PlaylistMusic>()
                .eq(PlaylistMusic::getPlaylistId, RankConstants.HOT_MUSIC_RANK_ID));

        // 将查询出的前100首音乐添加到热歌榜中
        addMusicToRank(RankConstants.HOT_MUSIC_RANK_ID, musicIds);
    }

    /**
     * 更新新歌榜
     * 每日凌晨1点10分0秒执行（错开热歌榜更新时间，降低并发压力）
     */
    @Scheduled(cron = "0 10 1 * * ?")
    public void updateNewMusicRank() {
        // 从数据库中查询按播放量从高到低排序一个月内发布的音乐id
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                        .select(Music::getId) // 仅查询ID字段，减少数据量
                        .ge(Music::getCreateTime, oneMonthAgo) // 发布时间 >= 一个月前
                        .orderByDesc(Music::getPlayCount) // 按播放量降序
                        .last("limit 100"), // 限制结果数量为100
                r -> ((Number) r).longValue()
        );
        if (playlistMapper.selectById(RankConstants.NEW_MUSIC_RANK_ID) == null) {
            // 如果新歌榜不存在，则创建一个新的新歌榜
            Playlist playlist = new Playlist();
            playlist.setId(RankConstants.NEW_MUSIC_RANK_ID);
            playlist.setName("新歌榜");
            playlist.setUserId(15L);
            playlist.setDescription("新歌榜：用户收听所有新歌（一月内最新发行） 官方TOP排行榜，每天更新。");
            playlist.setIsPublic(1);
            playlist.setMusicCount((long) musicIds.size());
            playlist.setPlayCount(0L);
            playlist.setCreatedTime(LocalDateTime.now());
            playlist.setUpdatedTime(LocalDateTime.now());
            playlist.setCollectCount(0L);
            playlist.setCommentCount(0L);

            playlistMapper.insert(playlist);

            addMusicToRank(RankConstants.NEW_MUSIC_RANK_ID, musicIds);

            return;
        }
        // 清空新歌榜中的所有音乐
        playlistMusicMapper.delete(new LambdaQueryWrapper<PlaylistMusic>()
                .eq(PlaylistMusic::getPlaylistId, RankConstants.NEW_MUSIC_RANK_ID));

        // 将查询出的前100首音乐添加到新歌榜中
        addMusicToRank(RankConstants.NEW_MUSIC_RANK_ID, musicIds);
    }

    /**
     * 更新VIP热歌榜
     * 每日凌晨1点20分0秒执行
     */
    @Scheduled(cron = "0 20 1 * * ?")
    public void updateVipHotMusicRank() {
        // 从数据库中根据播放量从高到低排序查询出前100首vip音乐的id
        List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                        .select(Music::getId) // 仅查询ID字段，减少数据量
                        .orderByDesc(Music::getPlayCount)
                        .eq(Music::getIsVip, 1) // 仅查询VIP音乐
                        .last("limit 50"),
                r -> ((Number) r).longValue()
        );
        if (playlistMapper.selectById(RankConstants.VIP_HOT_MUSIC_RANK_ID) == null) {
            // 如果VIP热歌榜不存在，则创建一个新的VIP热歌榜
            Playlist playlist = new Playlist();
            playlist.setId(RankConstants.VIP_HOT_MUSIC_RANK_ID);
            playlist.setName("VIP热歌榜");
            playlist.setUserId(15L);
            playlist.setDescription("VIP热歌榜：所有VIP线上歌曲官方TOP排行榜，每日更新。");
            playlist.setIsPublic(1);
            playlist.setMusicCount((long) musicIds.size());
            playlist.setPlayCount(0L);
            playlist.setCreatedTime(LocalDateTime.now());
            playlist.setUpdatedTime(LocalDateTime.now());
            playlist.setCollectCount(0L);
            playlist.setCommentCount(0L);

            playlistMapper.insert(playlist);

            addMusicToRank(RankConstants.VIP_HOT_MUSIC_RANK_ID, musicIds);

            return;
        }
        // 清空VIP热歌榜中的所有音乐
        playlistMusicMapper.delete(new LambdaQueryWrapper<PlaylistMusic>()
                .eq(PlaylistMusic::getPlaylistId, RankConstants.VIP_HOT_MUSIC_RANK_ID));

        // 将查询出的前100首音乐添加到VIP热歌榜中
        addMusicToRank(RankConstants.VIP_HOT_MUSIC_RANK_ID, musicIds);
    }

    /**
     * 更新VIP新歌榜
     * 每日凌晨1点30分0秒执行
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void updateVipNewMusicRank() {
        // 从数据库中查询按播放量从高到低排序一个月内发布的音乐id
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Long> musicIds = musicService.listObjs(new LambdaQueryWrapper<Music>()
                        .select(Music::getId) // 仅查询ID字段，减少数据量
                        .eq(Music::getIsVip, 1) // 仅查询VIP音乐
                        .ge(Music::getCreateTime, oneMonthAgo) // 发布时间 >= 一个月前
                        .orderByDesc(Music::getPlayCount) // 按播放量降序
                        .last("limit 50"), // 限制结果数量为50
                r -> ((Number) r).longValue()
        );
        if (playlistMapper.selectById(RankConstants.VIP_NEW_MUSIC_RANK_ID) == null) {
            // 如果新歌榜不存在，则创建一个新的新歌榜
            Playlist playlist = new Playlist();
            playlist.setId(RankConstants.VIP_NEW_MUSIC_RANK_ID);
            playlist.setName("VIP新歌榜");
            playlist.setUserId(15L);
            playlist.setDescription("VIP新歌榜：VIP线上新歌（一个月内最新发行）官方TOP排行榜，每日更新。");
            playlist.setIsPublic(1);
            playlist.setMusicCount((long) musicIds.size());
            playlist.setPlayCount(0L);
            playlist.setCreatedTime(LocalDateTime.now());
            playlist.setUpdatedTime(LocalDateTime.now());
            playlist.setCollectCount(0L);
            playlist.setCommentCount(0L);

            playlistMapper.insert(playlist);

            addMusicToRank(RankConstants.VIP_NEW_MUSIC_RANK_ID, musicIds);

            return;
        }
        // 清空VIP新歌榜中的所有音乐
        playlistMusicMapper.delete(new LambdaQueryWrapper<PlaylistMusic>()
                .eq(PlaylistMusic::getPlaylistId, RankConstants.VIP_NEW_MUSIC_RANK_ID));

        // 将查询出的前100首音乐添加到VIP新歌榜中
        addMusicToRank(RankConstants.VIP_NEW_MUSIC_RANK_ID, musicIds);
    }

    // 将音乐添加到榜单中
    private void addMusicToRank(Long rankId, List<Long> musicIds) {
        int sortOrder = 1;
        for (Long musicId : musicIds) {
            PlaylistMusic playlistMusic = new PlaylistMusic();
            playlistMusic.setPlaylistId(rankId);
            playlistMusic.setMusicId(musicId);
            playlistMusic.setSortOrder(sortOrder++);
            playlistMusic.setAddedTime(LocalDateTime.now());
            playlistMusicMapper.insert(playlistMusic);
        }
        // 更新榜单中的音乐数量
        Playlist playlist = playlistMapper.selectById(rankId);
        playlist.setMusicCount((long) musicIds.size());
        playlist.setUpdatedTime(LocalDateTime.now());
        playlistMapper.updateById(playlist);
    }

}
