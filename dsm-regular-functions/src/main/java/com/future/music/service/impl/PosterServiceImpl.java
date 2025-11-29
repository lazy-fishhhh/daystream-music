//package com.future.music.service.impl;
//
//import ai.djl.util.RandomUtils;
//import com.future.login.mapper.UserMapper;
//import com.future.music.domain.dto.PosterGenerateDTO;
//import com.future.music.domain.vo.PosterVO;
//import com.future.music.mapper.AlbumMapper;
//import com.future.music.mapper.MusicMapper;
//import com.future.music.mapper.MusicianMapper;
//import com.future.music.service.IMinIoService;
//import com.future.music.service.IPosterService;
//import entity.pojo.info.User;
//import entity.pojo.music.Album;
//import entity.pojo.music.Music;
//import entity.pojo.music.Musician;
//import entity.pojo.util.Result;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.geom.Ellipse2D;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//@Service
//public class PosterServiceImpl implements IPosterService {
//
//    @Autowired
//    private MusicMapper musicMapper;       // 歌曲信息
//    @Autowired
//    private UserMapper userMapper;         // 用户信息（昵称/头像）
//    @Autowired
//    private MusicianMapper musicianMapper; // 音乐人信息
//    @Autowired
//    private AlbumMapper albumMapper;       // 专辑封面
//    @Autowired
//    private IMinIoService minIoService;     // 图片存储（MinIO）
//
//    // 海报尺寸常量（宽×高，像素）
//    private static final int POSTER_WIDTH = 800;
//    private static final int POSTER_HEIGHT = 1200;
//    // 风格模板路径（可放在项目resources/templates/poster/下）
//    private static final String STYLE_TEMPLATE_PATH = "templates/poster/style_%d.png";
//
//    @Override
//    public Result<PosterVO> generatePoster(PosterGenerateDTO dto) {
//        // 1. 参数校验
//        if (dto.getSongId() == null || dto.getUserId() == null || dto.getStyle() == null) {
//            return Result.fail("参数不完整");
//        }
//        // 校验风格合法性（1-3）
//        if (dto.getStyle() < 1 || dto.getStyle() > 3) {
//            return Result.fail("不支持该海报风格");
//        }
//
//        try {
//            // 2. 查询基础数据（歌曲、用户、音乐人、专辑信息）
//            Music music = musicMapper.selectById(dto.getSongId());
//            User user = userMapper.selectById(dto.getUserId());
//            if (music == null || user == null) {
//                return Result.fail("歌曲或用户不存在");
//            }
//            Musician musician = musicianMapper.selectById(music.getMusicianId());
//            Album album = albumMapper.selectById(music.getAlbumId());
//
//            // 3. 加载风格模板图片（如简约风模板）
//            BufferedImage templateImage = loadStyleTemplate(dto.getStyle());
//            if (templateImage == null) {
//                return Result.fail("风格模板加载失败");
//            }
//
//            // 4. 动态绘制海报内容（覆盖到模板上）
//            BufferedImage posterImage = drawPosterContent(templateImage, music, user, musician, album);
//
//            // 5. 上传海报图片到MinIO，获取URL
//            String posterUrl = uploadPosterToMinIO(posterImage, dto.getSongId());
//            if (posterUrl == null) {
//                return Result.fail("海报生成失败");
//            }
//
//            // 6. 返回海报URL
//            PosterVO vo = new PosterVO();
//            vo.setPosterUrl(posterUrl);
//            return Result.ok(vo);
//
//        } catch (Exception e) {
//            return Result.fail("海报生成失败，请稍后重试");
//        }
//    }
//
//    /**
//     * 加载风格模板图片（从resources读取）
//     */
//    private BufferedImage loadStyleTemplate(int style) throws IOException {
//        String templatePath = STYLE_TEMPLATE_PATH.formatted(style);
//        try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
//            if (is == null) {
//                return null;
//            }
//            return ImageIO.read(is);
//        }
//    }
//
//    /**
//     * 绘制海报内容（核心：在模板上叠加歌曲信息、用户信息等）
//     */
//    private BufferedImage drawPosterContent(
//            BufferedImage template, Music music, User user, Musician musician, Album album) throws IOException {
//        // 1. 创建画布（基于模板尺寸）
//        BufferedImage poster = new BufferedImage(
//                POSTER_WIDTH, POSTER_HEIGHT, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = poster.createGraphics();
//        // 设置抗锯齿，让文字和图片更清晰
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        // 2. 绘制模板背景
//        g2d.drawImage(template, 0, 0, POSTER_WIDTH, POSTER_HEIGHT, null);
//
//        // 3. 绘制专辑封面（示例位置：顶部居中，尺寸400×400）
//        if (album != null && album.getCoverUrl() != null) {
//            // 从MinIO下载专辑封面（或直接读取URL）
//            BufferedImage albumCover = minIoService.downloadImage(album.getCoverUrl());
//            if (albumCover != null) {
//                // 缩放专辑封面并绘制（x=200, y=100, 宽400, 高400）
//                g2d.drawImage(
//                        albumCover.getScaledInstance(400, 400, Image.SCALE_SMOOTH),
//                        200, 100, null
//                );
//            }
//        }
//
//        // 4. 绘制歌曲名（示例位置：专辑封面下方，字体大小36）
//        g2d.setColor(Color.WHITE); // 文字颜色（根据模板调整）
//        g2d.setFont(new Font("微软雅黑", Font.BOLD, 36));
//        String songName = music.getMusicName();
//        // 文字居中绘制（x=400-文字宽度/2, y=600）
//        int songNameX = (POSTER_WIDTH - g2d.getFontMetrics().stringWidth(songName)) / 2;
//        g2d.drawString(songName, songNameX, 600);
//
//        // 5. 绘制音乐人名称（示例位置：歌曲名下方，字体大小28）
//        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 28));
//        String musicianName = musician != null ? musician.getStageName() : "未知音乐人";
//        int musicianX = (POSTER_WIDTH - g2d.getFontMetrics().stringWidth(musicianName)) / 2;
//        g2d.drawString(musicianName, musicianX, 650);
//
//        // 6. 绘制分享用户信息（示例位置：底部，用户头像+昵称）
//        // 6.1 绘制用户头像（圆形，x=350, y=1000, 直径100）
//        if (user.getAvatar() != null) {
//            BufferedImage avatar = minIoService.downloadImage(user.getAvatar());
//            if (avatar != null) {
//                // 绘制圆形头像（需要处理圆角）
//                Shape circle = new Ellipse2D.Float(350, 1000, 100, 100);
//                g2d.setClip(circle);
//                g2d.drawImage(
//                        avatar.getScaledInstance(100, 100, Image.SCALE_SMOOTH),
//                        350, 1000, null
//                );
//                g2d.setClip(null); // 取消裁剪
//            }
//        }
//        // 6.2 绘制用户昵称（头像右侧，y=1050）
//        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 24));
//        String nickname = user.getUsername() + " 分享";
//        g2d.drawString(nickname, 470, 1050);
//
//        // 7. 释放资源
//        g2d.dispose();
//        return poster;
//    }
//
//    /**
//     * 上传海报图片到MinIO，返回URL
//     */
//    private String uploadPosterToMinIO(BufferedImage image, Long songId) throws IOException {
//        // 将BufferedImage转为InputStream
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        ImageIO.write(image, "png", os);
//        InputStream is = new ByteArrayInputStream(os.toByteArray());
//
//        // 生成文件名（songId+时间戳+随机数.png）
//        String fileName = "poster/song_%d_%d_%s.png".formatted(
//                songId, System.currentTimeMillis(), RandomUtils.randomString(6)
//        );
//
//        // 上传到MinIO
//        return minIoService.uploadFile(is, fileName, "image/png");
//    }
//}