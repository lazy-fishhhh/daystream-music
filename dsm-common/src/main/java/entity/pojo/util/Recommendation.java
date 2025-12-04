package entity.pojo.util;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
     * CF辅助类：存储推荐物品的ID和对应的推荐得分
     * 作用：封装物品-得分对，便于排序和返回给上层业务
     */
@Data
@NoArgsConstructor
public class Recommendation {
    private Long itemId;       // 歌曲ID
    public Double score;      // 兴趣相似度得分（协同过滤专用，热门歌曲置null）
    public Double hotScore;     // 热门得分（热门歌曲专用，协同过滤置null）
    private String songName;   // 歌曲名
    private String singer;     // 歌手
    private String coverUrl;   // 封面
        /**
         * 构造方法：初始化推荐结果
         * @param itemId 物品ID
         * @param score 推荐得分
         */
        public Recommendation(Long itemId, double score) {
            this.itemId = itemId;
            this.score = score;
        }

        /**
         * 重写toString：友好展示推荐结果（便于测试/日志输出）
         * 格式：(物品ID: 105, 推荐得分: 0.667)
         */
        @Override
        public String toString() {
            return String.format("(物品ID: %d, 推荐得分: %.3f)", itemId, score);
        }
    }