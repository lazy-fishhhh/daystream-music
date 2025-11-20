package com.future.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.future.login.domain.LoginUserDetails;
import com.future.login.util.SecurityUtil;
import com.future.music.domain.vo.MusicDetailVO;
import com.future.music.domain.vo.PlaylistDetailVO;
import com.future.music.mapper.*;
import com.future.music.service.IUserService;
import entity.dto.PlayListDTO;
import entity.pojo.music.Album;
import entity.pojo.music.Music;
import entity.pojo.music.Musician;
import entity.pojo.music.Playlist;
import entity.pojo.social.LikeRecord;
import entity.pojo.social.UserCollection;
import entity.pojo.util.PageReq;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import util.AudioDurationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.future.music.util.constants.CHECK_IN_KEY;


@RequiredArgsConstructor
@Service
public class UserServiceImpl  implements IUserService {

    @Resource
    private PlaylistMusicMapper playlistMusicMapper;

    @Resource
    private LikeRecordMapper likeRecordMapper;

    @Resource
    private UserCollectionMapper userCollectionMapper;

    @Resource
    private AlbumMapper albumMapper;

    @Resource
    private MusicianMapper musicianMapper;

    //构造方法注入
    private final StringRedisTemplate stringRedisTemplate;
    private final PlayListMapper playListMapper;
    @Override
    public void createPlayList(PlayListDTO playListDTO) {

        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getName, playListDTO.getName());
        Playlist playlist = playListMapper.selectOne(lqw);
        if (playlist != null){
            throw new RuntimeException("歌单名称已存在,不允许重复");
        }

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

    @Override
    public void deletePlayList(long playListId) {
        LambdaQueryWrapper<Playlist> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Playlist::getId, playListId);
        Playlist playlist = playListMapper.selectOne(lqw);
        if (playlist == null){
            throw new RuntimeException("歌单不存在");
        }

        LoginUserDetails loginUser = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = loginUser.getUser().getId();
        Long playListUserId = playlist.getUserId();

        if (!playListUserId.equals(userId)) {
            throw new RuntimeException("您没有权限删除此歌单");
        }

        playListMapper.deleteById(playListId);
    }

    @Override
    public Result<String> checkIn() {
        // 1. 获取当前登录的用户
        Long userId = SecurityUtil.getCurrentUserId();

        // 2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3. 拼接Key
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = CHECK_IN_KEY + userId + ":" + keySuffix;
        // 4. 获取今天是本月的第几天
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int offset = dayOfMonth - 1; // 偏移量，从0开始所以要-1
        
        // 5. 检查用户是否已经签到
        // Redis的getBit命令：获取键对应偏移量的bit值（1=已签到，0=未签到）
        // 若键不存在，返回null（需通过Boolean.TRUE.equals判断，避免空指针）
        Boolean isCheckedIn = stringRedisTemplate.opsForValue().getBit(key, offset);
        
        // 当key不存在时，getBit返回null，此时需要特殊处理

        if (Boolean.TRUE.equals(isCheckedIn)) {
            return Result.fail(400, "今日您已签到过了,明日再来吧~");
        }

        // 6. 写入Redis SETBIT key offset 1
        // 将当前偏移量的bit值设为1（表示已签到）
        // SETBIT命令：原子操作，确保并发场景下不会重复签到
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        
//        // 7. 设置过期时间-到本年年底，避免数据永久存储
//        if (!stringRedisTemplate.hasKey(key)) {
//            // 计算本年底最后一天
//            LocalDate endOfYear = LocalDate.now().withMonth(12).withDayOfMonth(31);
//            // 计算从今天到年底的天数（加1确保包含年底当天），设置为过期时间
//            long expireDays = ChronoUnit.DAYS.between(LocalDate.now(), endOfYear) + 1;
//            stringRedisTemplate.expire(key, expireDays, TimeUnit.DAYS);
//        }
        
        return Result.ok("签到成功");
    }

    @Override
    public Result<Integer> getContinuousSignInDays() {
        // 1.获取当前用户
        Long userId = SecurityUtil.getCurrentUserId();

        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 3. 拼接日期
        String key = CHECK_IN_KEY + userId + ":" + keySuffix;

        // 4. 获取今天是本月的第几天
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int offset = dayOfMonth - 1; // 偏移量，从0开始所以要-1

        // 5. 获取本月截至今天为止的所有签到记录，返回的是一个十进制数字
        // BITFIELD key GET uoffset offset
        // uoffset：unsigned offset，无符号偏移量，用于指定要都到哪里
        // offset：指定从哪里开始读取
        // 从0开始读取，读取到offset位置

        //多个子命令多返回值，所以返回一个List
        List<Long> signInRecord = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(offset))//GET TYPE
                        .valueAt(0)//从0开始读取，读取到offset位置的bit值    //OFFSET
        );
        if(signInRecord== null || signInRecord.isEmpty()){
            //没有任何签到结果
            return Result.ok(0);
        }
        //list里只有一个元素，就是签到记录，所以get(0)
        Long num = signInRecord.get(0);
        if(num == null || num == 0){
            //没有任何签到结果
            return Result.ok(0);
        }

        // 6.循环遍历所有签到记录，判断是否连续签到
        int continuousDays = 0;
        // 从右向左遍历，因为是从0开始读取，所以要从右向左遍历
        // 不是1，说明签到中断
        // 是1，说明连续签到
        while ((num & 1) != 0) {
            // 判断当前位(最右边的位)是否为1
            continuousDays++;
            // 右移一位，检查下一位
            // 整数进行右移一位（num >> 1）时，二进制数整体向右移动
            // 原来的最右边的第 0 位会被丢弃，原来的第 1 位会变成新的第 0 位（最右边）
            num = num >> 1;
        }

        return Result.ok(continuousDays);
    }

    /**
     * 根据歌单ID查询歌单详情（包含基本信息和关联歌曲ID）
     * @param playlistId 歌单ID
     * @return 歌单详情（含歌曲ID列表），若不存在则返回null
     */
    public Result<PlaylistDetailVO> getPlaylistDetail(Long playlistId, int pageNum, int pageSize) {
        if (playlistId == null || playlistId <= 0) {
            return Result.fail("歌单ID出错");
        }

        // 1. 查询歌单基本信息
        Playlist playlist = playListMapper.selectById(playlistId);
        if (playlist == null) {
            return Result.fail("歌单不存在");
        }

        // 2. 查询歌单包含的歌曲ID列表（关联表查询）
        Page<Music> page = new Page<>(pageNum,pageSize);
        IPage<Music> musicPage = playlistMusicMapper.selectMusicsByPlaylistId(page, playlistId);

        // 3. 封装成VO（视图对象，用于返回给前端）
        PlaylistDetailVO detailVO = new PlaylistDetailVO();
        // 复制基本属性（使用BeanUtils简化代码）
        BeanUtils.copyProperties(playlist, detailVO);

        List<MusicDetailVO> voList = new ArrayList<>();
        for (Music music : musicPage.getRecords()) {
            MusicDetailVO vo = new MusicDetailVO();
            vo.copyFrom(music);

            // 转换时长
            vo.setDuration(AudioDurationUtil.formatSecondsToMS(music.getDuration()));

            // 5.1 关联专辑名称（通过albumId查album表）
            Album album = albumMapper.selectById(music.getAlbumId());
            if (album != null) {
                vo.setAlbumName(album.getAlbumName());
            }

            // 5.2 关联点赞、收藏状态
            if (SecurityUtil.getCurrentUserId() != null) {
                QueryWrapper<LikeRecord> likeWrapper = new QueryWrapper<>();
                likeWrapper.eq("target_id", music.getId())
                        .eq("target_type", 1)
                        .eq("user_id", SecurityUtil.getCurrentUserId());
                LikeRecord likeRecord = likeRecordMapper.selectOne(likeWrapper);
                vo.setIsLiked(likeRecord != null ? 1 : 0);

                QueryWrapper<UserCollection> collectionWrapper = new QueryWrapper<>();
                collectionWrapper.eq("user_id", SecurityUtil.getCurrentUserId())
                        .eq("target_type", 1)
                        .eq("target_id", music.getId());
                UserCollection userCollection = userCollectionMapper.selectOne(collectionWrapper);
                vo.setIsCollected(userCollection != null ? 1 : 0);
            } else {
                vo.setIsLiked(0); // 未登录默认未点赞
                vo.setIsCollected(0);
            }

            // 5.4 关联音乐人名字（通过musicianId查musician表）
            Musician musician = musicianMapper.selectById(music.getMusicianId());
            if (musician != null) {
                vo.setMusicianName(musician.getStageName());
            }

            voList.add(vo);
        }

        PageResp<MusicDetailVO> pageResult = new PageResp<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(musicPage.getTotal());
        pageResult.setPages((int) musicPage.getPages());

        detailVO.setMusicDetailVOList(pageResult);

        return Result.ok(detailVO);
    }

}