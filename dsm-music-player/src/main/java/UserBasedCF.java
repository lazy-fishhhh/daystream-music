import java.util.*;

public class UserBasedCF {
    
    // 用户行为数据：用户ID -> 听过的歌曲集合
    private Map<String, Set<String>> userBehavior;
    
    public UserBasedCF(Map<String, Set<String>> userBehavior) {
        // 参数中的userBehavior是通过数据库查询所有用户行为（听过的歌、喜欢的歌）数据生成的
        this.userBehavior = userBehavior;
    }
    
    /**
     * 计算用户相似度（余弦相似度）
     * wij = |N(i) ∩ N(j)| / √(|N(i)| × |N(j)|)
     */
    public double calculateSimilarity(String user1, String user2) {
        // 通过userId 获取 该用户听过的歌曲的集合
        Set<String> items1 = userBehavior.get(user1);
        Set<String> items2 = userBehavior.get(user2);
        
        if (items1 == null || items2 == null) {
            return 0.0;
        }
        
        // 计算交集大小
        Set<String> intersection = new HashSet<>(items1);
        // retainAll方法：保留集合中与参数集合相同的元素
        intersection.retainAll(items2);
        int commonItems = intersection.size();
        
        // 计算余弦相似度
        if (commonItems == 0) {
            return 0.0;
        }


        /**
         *  commonItems:两个行为集合的交集大小(如用户 A 和用户 B 都听过的歌曲数量）;
         *  items1.size() * items2.size() ：两个行为集合总数量的乘积（计算向量模长的平方）
         *  * 1.0 ：强制将整数乘积转为浮点数（避免 Java 中整数除法导致的精度丢失
         */
        return commonItems / Math.sqrt(items1.size() * items2.size() * 1.0);

    }
    
    /**
     * 找到最相似的K个用户
     */
    public List<UserSimilarity> findTopKSimilarUsers(String targetUser, int k) {
        List<UserSimilarity> similarities = new ArrayList<>();
        
        for (String otherUser : userBehavior.keySet()) {
            if (!otherUser.equals(targetUser)) {
                double similarity = calculateSimilarity(targetUser, otherUser);
                similarities.add(new UserSimilarity(otherUser, similarity));
            }
        }
        
        // 按相似度降序排序
        similarities.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        
        // 返回前K个
        return similarities.subList(0, Math.min(k, similarities.size()));
    }
    
    /**
     *
     * 生成推荐（基于文章中的公式）
     * p(u,i) = Σ[v∈S(u,K)∩N(i)] W_uv × r_vi
     * 其中 r_vi = 1（隐式反馈）
     *   S(u,K) ：包含和用户u兴趣最接近的K个用户
     *   N(i) ：对物品i有过行为的用户集合
     * W_uv ：用户u 和用户v 的兴趣相似度
     * r_vi ：代表用户v 对物品的兴趣
     * 因为我们这里是有听过\没听过 or 喜欢\不喜欢 ，只有 0 1两种情况
     * 所以使用的是单一行为的隐反馈数据，因此其中的 r_vi = 1（隐式反馈）
     * 如果是评分机制的数据,比如说虎扑评分有12345,则 r_vi = r_vi，就是皮尔逊相关系数的体系了
     *
     */


    /**
     *
     * @param targetUser
     * @param k  参考最相似的k个用户
     * @param n  返回n首最可能喜欢的item
     * @return
     */
    public List<Recommendation> recommend(String targetUser, int k, int n) {
        Set<String> targetUserItems = userBehavior.get(targetUser);
        List<UserSimilarity> similarUsers = findTopKSimilarUsers(targetUser, k);

        Map<String, Double> recommendationScores = new HashMap<>();

        for (UserSimilarity similarUser : similarUsers) {
            String similarUserId = similarUser.userId;
            double similarity = similarUser.similarity;

            Set<String> similarUserItems = userBehavior.get(similarUserId);

            for (String item : similarUserItems) {
                if (!targetUserItems.contains(item)) {
                    recommendationScores.merge(item, similarity, Double::sum);
                }
            }
        }

        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<String, Double> entry : recommendationScores.entrySet()) {
            recommendations.add(new Recommendation(entry.getKey(), entry.getValue()));
        }

        // 按分数降序排序
        recommendations.sort((a, b) -> Double.compare(b.score, a.score));

        // 截取前n个结果（处理n大于总数量的边界情况）
        int returnSize = Math.min(n, recommendations.size());
        return recommendations.subList(0, returnSize);
    }
    
    // 辅助类
    static class UserSimilarity {
        String userId;
        double similarity;
        
        public UserSimilarity(String userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
    }
    
    static class Recommendation {
        String itemId;
        double score;
        
        public Recommendation(String itemId, double score) {
            this.itemId = itemId;
            this.score = score;
        }
        
        @Override
        public String toString() {
            return String.format("(%s, %.3f)", itemId, score);
        }
    }
    
    // 测试示例
    public static void main(String[] args) {
        // 构建测试数据（与文章中的例子对应）
        Map<String, Set<String>> userBehavior = new HashMap<>();
        
        // 用户A听过a,b,c
        userBehavior.put("A", new HashSet<>(Arrays.asList("a", "b", "c")));
        // 用户B听过a,d
        userBehavior.put("B", new HashSet<>(Arrays.asList("a", "d")));
        // 用户C听过a,b,e
        userBehavior.put("C", new HashSet<>(Arrays.asList("a", "b", "e")));
        // 用户D听过c,d,e
        userBehavior.put("D", new HashSet<>(Arrays.asList("c", "d", "e")));
        
        UserBasedCF recommender = new UserBasedCF(userBehavior);
        
        // 测试相似度计算（验证文章中的例子）
        System.out.println("A和D的相似度: " + recommender.calculateSimilarity("A", "D"));
        // 应该输出 0.333，与文章中一致：1/√(3*3) = 1/3 ≈ 0.333
        
        // 生成推荐
        List<Recommendation> recommendations = recommender.recommend("A",  3 ,    3);
        // 返回结果只有两首（e和d）是完全符合预期的
        // 原因是：符合 “相似用户听过、但用户 A 没听过” 的歌曲只有这两首,没有更多符合条件的候选歌曲
        System.out.println("给用户A的推荐: " + recommendations);
    }
}