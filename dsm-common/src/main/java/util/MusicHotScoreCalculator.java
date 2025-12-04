package util;


import entity.vo.MusicRecommendVO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 歌曲热门度权重计算工具类
 * 核心：融合多维度指标 + 时间衰减，保证热门歌曲的时效性
 */
public class MusicHotScoreCalculator {

    // 权重配置（可配置化，建议放到配置文件）
    private static final double PLAY_COUNT_WEIGHT = 0.5;    // 播放量权重
    private static final double LIKE_COUNT_WEIGHT = 0.2;    // 点赞数权重
    private static final double COMMENT_COUNT_WEIGHT = 0.15;// 评论数权重
    private static final double DOWNLOAD_COUNT_WEIGHT = 0.15;// 下载量权重

    // 时间衰减系数（发布超过30天的歌曲，热度衰减）
    private static final int DECAY_DAYS = 30;
    private static final double DECAY_FACTOR = 0.8;         // 衰减因子（<1表示衰减）

    /**
     * 计算单首歌曲的热门得分
     * @param musicVO 歌曲详情（含播放量、点赞数、发布时间等）
     * @return 归一化后的热门得分（0~100）
     */

    public static double calculateHotScore(MusicRecommendVO musicVO) {
        if (musicVO == null) {
            return 0.0;
        }

        // 1. 基础指标（空值兜底）
        long playCount = Optional.ofNullable(musicVO.getPlayCount()).orElse(0L);
        long likeCount = Optional.ofNullable(musicVO.getLikeCount()).orElse(0L);
        long commentCount = Optional.ofNullable(musicVO.getCommentCount()).orElse(0L);
        long downloadCount = Optional.ofNullable(musicVO.getDownloadCount()).orElse(0L);
        LocalDateTime createTime = Optional.ofNullable(musicVO.getCreateTime()).orElse(LocalDateTime.now());

        // 2. 指标归一化（避免数值过大导致得分失衡，按平台最大值归一）
        double normPlay = normalize(playCount, 100000000L);       // 播放量最大值按1亿归一
        double normLike = normalize(likeCount, 1000000L);         // 点赞数最大值按100万归一
        double normComment = normalize(commentCount, 100000L);    // 评论数最大值按10万归一
        double normDownload = normalize(downloadCount, 500000L);  // 下载量最大值按50万归一

        // 3. 基础得分（加权求和）
        double baseScore = normPlay * PLAY_COUNT_WEIGHT
                + normLike * LIKE_COUNT_WEIGHT
                + normComment * COMMENT_COUNT_WEIGHT
                + normDownload * DOWNLOAD_COUNT_WEIGHT;

        // 4. 时间衰减（发布越久，热度越低）
        long daysSincePublish = Duration.between(createTime, LocalDateTime.now()).toDays();
        double timeDecay = daysSincePublish > DECAY_DAYS ? Math.pow(DECAY_FACTOR, daysSincePublish - DECAY_DAYS) : 1.0;

        // 5. 最终得分（放大到0~100区间，便于理解）
        return baseScore * timeDecay * 100;
    }

    /**
     * 指标归一化（0~1）
     * @param value 原始值
     * @param maxValue 该指标的最大值（平台历史峰值）
     * @return 归一化后的值
     */
    private static double normalize(long value, long maxValue) {
        if (maxValue == 0 || value <= 0) {
            return 0.0;
        }
        // 对数归一：避免极端值（如播放量1亿和100万差距过大）
        return Math.log10(1 + value) / Math.log10(1 + maxValue);
    }
}