package util;

import entity.pojo.util.Recommendation;

import java.util.*;

/**
 * 基于用户的协同过滤（User-Based Collaborative Filtering）推荐算法实现
 * 核心思想：
 * 1. 计算目标用户与其他用户的余弦相似度，找到最相似的K个用户（近邻）
 * 2. 收集近邻用户喜欢但目标用户未接触的物品，按相似度加权得分排序
 * 3. 返回得分最高的N个物品作为推荐结果
 * - 相似度计算：采用余弦相似度，适配隐式反馈（喜欢/不喜欢）的用户行为数据
 */

public class UserBasedCF {

    // 用户行为数据存储：key=用户ID(Long)，value=该用户有过交互（听过）的物品ID集合(Long)
    // 注：隐式反馈场景下，集合中的物品代表用户"喜欢/感兴趣"，无负面反馈
    private Map<Long, Set<Long>> userBehavior;

    /**
     * 构造方法：初始化用户行为数据
     * @param userBehavior 从数据库/缓存加载的全量用户行为数据（用户-物品交互集合）
     *                     数据来源：通常是用户听歌、收藏、点赞等行为的持久化数据
     */
    public UserBasedCF(Map<Long, Set<Long>> userBehavior) {
        // 直接引用外部传入的行为数据，避免深拷贝的性能损耗（需线程安全改为深拷贝）
        this.userBehavior = userBehavior;
    }

    /**
     * 计算两个用户的兴趣相似度（余弦相似度）
     * 公式：wij = |N(i) ∩ N(j)| / √(|N(i)| × |N(j)|)
     * 其中：
     * - N(i)：用户i的行为物品集合，|N(i)|为集合大小
     * - |N(i) ∩ N(j)|：两个用户共同交互的物品数量（交集大小）
     * @param user1 第一个用户ID
     * @param user2 第二个用户ID
     * @return 0~1之间的相似度值（0=无相似性，1=完全相似）
     */
    public double calculateSimilarity(Long user1, Long user2) {
        // 1. 获取两个用户的行为物品集合
        Set<Long> items1 = userBehavior.get(user1);
        Set<Long> items2 = userBehavior.get(user2);

        // 若任一用户无行为数据，相似度为0（无交互则无法计算相似性）
        if (items1 == null || items2 == null) {
            return 0.0;
        }

        // 2. 计算两个用户行为集合的交集大小（共同喜欢的歌曲数量）
        Set<Long> intersection = new HashSet<>(items1);
        // retainAll：保留与items2的交集元素，最终intersection即为交集歌曲集合
        intersection.retainAll(items2);
        int intersectionItems = intersection.size();

        // 3. 无交集歌曲时，相似度直接为0（避免分母为0）
        if (intersectionItems == 0) {
            return 0.0;
        }

        /**
         * 4. 余弦相似度核心计算：
         * - 分子：共同歌曲数量（向量点积，因行为向量为0/1值）
         * - 分母：两个用户行为集合大小的乘积开平方（向量模长的乘积）
         * - ×1.0：强制转为浮点数，避免Java整数除法导致的精度丢失（如1/3=0而非0.333）
         */
        double denominator = Math.sqrt(items1.size() * items2.size() * 1.0);
        return intersectionItems / denominator;
    }

    /**
     * 找到与目标用户最相似的前K个用户（近邻用户）
     * @param targetUser 目标用户ID
     * @param k 近邻数量（超参数，QQ音乐取的是30）
     * @return 按相似度降序排列的前K个相似用户列表
     */
    public List<UserSimilarity> findTopKSimilarUsers(Long targetUser, int k) {
        // 存储目标用户与所有其他用户的相似度
        List<UserSimilarity> similarities = new ArrayList<>();

        // 遍历全量用户，计算与目标用户的相似度（排除用户自身）
        for (Long otherUser : userBehavior.keySet()) {
            // 跳过目标用户自身（自身相似度无意义）
            if (!otherUser.equals(targetUser)) {
                double similarity = calculateSimilarity(targetUser, otherUser);
                // 相似度为0,这属于无贡献数据,不要加入集合
                if (similarity == 0.0) {
                    continue;
                }
                similarities.add(new UserSimilarity(otherUser, similarity));
            }
        }

        // 按相似度降序排序：相似度越高，优先级越高
        // 注：使用Double.compare避免NaN/Infinity导致的排序异常
        similarities.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 截取前K个用户：处理k大于用户总数的边界情况（Math.min避免下标越界）
        int topKSize = Math.min(k, similarities.size());
        return similarities.subList(0, topKSize);
    }

    /**
     * 推荐算法核心逻辑：为目标用户生成Top-N推荐歌曲
     * 公式：p(u,i) = Σ[v∈S(u,K)∩N(i)] W_uv × r_vi
     * 参数说明：
     * - S(u,K)：目标用户u的Top-K相似用户集合
     * - N(i)：对物品i有行为的用户集合
     * - W_uv：用户u与v的相似度
     * - r_vi：用户v对歌曲i的兴趣值（隐式反馈下r_vi=1，显式评分则为具体分数）
     * @param targetUser 目标用户ID
     * @param k 参考的相似用户数量（近邻数）
     * @param n 最终返回的推荐物品数量
     * @return 按推荐得分降序排列的推荐结果列表
     */
    public List<Recommendation> recommend(Long targetUser, int k, int n) {
        // 1. 获取目标用户已交互的歌曲集合（核心：避免推荐用户已喜欢过的歌曲）
        Set<Long> targetUserItems = userBehavior.get(targetUser);

        // 边界处理1：目标用户无行为数据，无法生成推荐，返回空列表
        if (targetUserItems == null || targetUserItems.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取目标用户的Top-K相似用户（近邻）
        List<UserSimilarity> similarUsers = findTopKSimilarUsers(targetUser, k);

        // 边界处理2：无相似用户（如全量用户仅目标用户自己），返回空列表
        if (similarUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 初始化推荐得分容器：key=物品ID，value=物品的总推荐得分（相似度加权和）
        // 注：HashMap非线程安全，但该方法为单线程调用，无需同步
        Map<Long, Double> recommendationScores = new HashMap<>();

        // 4. 遍历每个相似用户，计算候选物品的推荐得分
        for (UserSimilarity similarUser : similarUsers) {
            Long similarUserId = similarUser.userId;   // 相似用户ID
            double similarity = similarUser.similarity; // 与目标用户的相似度

            // 获取相似用户的行为物品集合
            Set<Long> similarUserItems = userBehavior.get(similarUserId);

            // 跳过无行为数据的相似用户
            if (similarUserItems == null || similarUserItems.isEmpty()) {
                continue;
            }

            // 遍历相似用户的物品，筛选目标用户未接触的物品
            for (Long item : similarUserItems) {
                // 核心过滤逻辑：仅推荐目标用户未听过的歌曲
                if (!targetUserItems.contains(item)) {
                    /**
                     * 累加物品得分：
                     * - merge方法：若物品未在Map中，初始值为similarity；若已存在，累加相似度
                     * - 等价于：recommendationScores.put(item, recommendationScores.getOrDefault(item, 0.0) + similarity)
                     * - 隐式反馈下r_vi=1，因此得分=ΣW_uv×1=ΣW_uv
                     */
                    recommendationScores.merge(item, similarity, Double::sum);
                }
            }
        }

        // 5. 将得分Map转换为推荐结果列表（便于排序和返回）
        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : recommendationScores.entrySet()) {
            recommendations.add(new Recommendation(entry.getKey(), entry.getValue()));
        }

        // 6. 按推荐得分降序排序：得分越高，推荐优先级越高
        // 若得分相同，歌曲ID顺序不保证（可补充按物品热度/流行度排序）
        recommendations.sort((a, b) -> Double.compare(b.score, a.score));

        // 7. 处理边界情况：候选物品数小于n时返回全部，否则返回前n个
        int returnSize = Math.min(n, recommendations.size());
        /**
         * 注意：subList返回的是原列表的视图，非新列表
         * 若需持久化/多线程操作，需改为：new ArrayList<>(recommendations.subList(0, returnSize))
         */
        return recommendations.subList(0, returnSize);
    }

    /**
     * 辅助类：存储相似用户的ID和对应的相似度值
     * 作用：封装用户-相似度对，便于列表存储和排序
     */
    static class UserSimilarity {
        Long userId;       // 相似用户ID（Long类型适配数据库BIGINT）
        double similarity; // 与目标用户的余弦相似度（0~1）

        /**
         * 构造方法：初始化相似用户数据
         * @param userId 相似用户ID
         * @param similarity 余弦相似度值
         */
        public UserSimilarity(Long userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
    }



    /**
     * 测试主方法：验证算法核心逻辑
     * 测试数据说明：
     * - 用户映射：A=1, B=2, C=3, D=4
     * - 物品映射：a=101, b=102, c=103, d=104, e=105
     * 预期结果：
     * - A(1)和D(4)的相似度：1/√(3×3)=1/3≈0.333
     * - 给A(1)的推荐：e(105)、d(104)（仅这两个物品符合条件）
     */
    public static void main(String[] args) {
        // 1. 构建测试用的用户行为数据
        Map<Long, Set<Long>> userBehavior = new HashMap<>();

        // 用户1：听过歌曲101(a)、102(b)、103(c)
        userBehavior.put(1L, new HashSet<>(Arrays.asList(101L, 102L, 103L)));
        // 用户2：听过歌曲101(a)、104(d)
        userBehavior.put(2L, new HashSet<>(Arrays.asList(101L, 104L)));
        // 用户3：听过歌曲101(a)、102(b)、105(e)
        userBehavior.put(3L, new HashSet<>(Arrays.asList(101L, 102L, 105L)));
        // 用户4：听过歌曲103(c)、104(d)、105(e)
        userBehavior.put(4L, new HashSet<>(Arrays.asList(103L, 104L, 105L)));

        // 2. 初始化推荐器
        UserBasedCF recommender = new UserBasedCF(userBehavior);

        // 3. 测试相似度计算：验证A(1)和D(4)的相似度
        double similarityAD = recommender.calculateSimilarity(1L, 4L);
        System.out.println("用户1和用户4的相似度: " + String.format("%.3f", similarityAD));
        // 预期输出：0.333

        // 4. 测试推荐逻辑：为用户1(原A)推荐Top3物品（实际仅2个符合条件）
        List<Recommendation> recommendations = recommender.recommend(1L, 3, 3);
        System.out.println("给用户1的推荐: " + recommendations);
        // 预期输出：[(物品ID: 105, 推荐得分: 0.667), (物品ID: 104, 推荐得分: 0.577)]
        // 得分计算说明：
        // - 105(e)：用户3(相似度≈0.816) + 用户4(相似度≈0.333) → 0.816+0.333≈1.149？实际需重新计算：
        //   修正：用户1与用户2相似度=1/√(3×2)=√6/6≈0.408；用户1与用户3相似度=2/√(3×3)=2/3≈0.667；用户1与用户4相似度=1/3≈0.333
        //   105(e)：用户3的相似度0.667 → 得分0.667
        //   104(d)：用户2(0.408) + 用户4(0.333) → 0.741（因浮点数精度，实际输出≈0.741）
    }
}