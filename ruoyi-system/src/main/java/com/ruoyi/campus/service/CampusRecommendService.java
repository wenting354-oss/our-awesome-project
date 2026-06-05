package com.ruoyi.campus.service;

import com.ruoyi.campus.domain.CampusProduct;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CampusRecommendService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ICampusProductService productService; // 借用你已有的商品查询服务

    /**
     * 记录用户行为（埋点）
     */
    public void recordBehavior(Long productId, Integer behaviorType) {
        Long userId = SecurityUtils.getUserId();
        // 设置权重：1=浏览(1分)，2=收藏(3分)，3=购买(5分)
        int score = behaviorType == 1 ? 1 : (behaviorType == 2 ? 3 : 5);

        String sql = "INSERT INTO campus_user_behavior (user_id, product_id, behavior_type, weight_score, create_time) VALUES (?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, userId, productId, behaviorType, score);
    }

    /**
     * 核心算法：基于物品的协同过滤 (Item-CF)
     * 猜你喜欢推荐列表
     */
    public List<CampusProduct> getRecommendProducts(int topN) {
        Long currentUserId = SecurityUtils.getUserId();

        // 1. 从数据库拉取所有用户的行为评分数据
        String sql = "SELECT user_id, product_id, SUM(weight_score) as total_score FROM campus_user_behavior GROUP BY user_id, product_id";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 数据结构转换：外层Key是商品ID，内层Map是 <用户ID, 评分>
        Map<Long, Map<Long, Double>> itemUserMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            Long productId = ((Number) row.get("product_id")).longValue();
            Double score = ((Number) row.get("total_score")).doubleValue();

            itemUserMap.computeIfAbsent(productId, k -> new HashMap<>()).put(userId, score);
        }

        // 2. 获取当前用户交互过的商品列表
        Set<Long> userInteractedItems = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : itemUserMap.entrySet()) {
            if (entry.getValue().containsKey(currentUserId)) {
                userInteractedItems.add(entry.getKey());
            }
        }

        // 💡 兜底策略：如果当前用户是新用户（没看过任何东西），直接返回最新发布的商品
        if (userInteractedItems.isEmpty()) {
            CampusProduct query = new CampusProduct();
            // 这里调用你原本的商品列表查询，由于是兜底，通常可以按时间倒序
            List<CampusProduct> newList = productService.selectCampusProductList(query);
            return newList.stream().limit(topN).collect(Collectors.toList());
        }

        // 3. 计算商品相似度并预测当前用户对未交互商品的评分
        Map<Long, Double> predictScores = new HashMap<>();

        for (Long candidateItem : itemUserMap.keySet()) {
            // 只预测用户没看过的商品
            if (userInteractedItems.contains(candidateItem)) continue;

            double totalPredictScore = 0.0;
            Map<Long, Double> candidateUsers = itemUserMap.get(candidateItem);

            for (Long interactedItem : userInteractedItems) {
                Map<Long, Double> interactedUsers = itemUserMap.get(interactedItem);

                // 计算两个商品之间的余弦相似度 (Cosine Similarity)
                double sim = calculateCosineSimilarity(candidateUsers, interactedUsers);

                // 预测评分 = 相似度 * 用户对已交互商品的实际评分
                Double userActualScore = interactedUsers.get(currentUserId);
                totalPredictScore += sim * userActualScore;
            }
            if (totalPredictScore > 0) {
                predictScores.put(candidateItem, totalPredictScore);
            }
        }

        // 4. 按预测分数倒序排序，取前 N 个商品ID
        List<Long> recommendItemIds = predictScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. 根据商品ID查询详细信息并返回
        if (recommendItemIds.isEmpty()) return new ArrayList<>();

        List<CampusProduct> recommendList = new ArrayList<>();
        for (Long id : recommendItemIds) {
            recommendList.add(productService.selectCampusProductByProductId(id));
        }
        return recommendList;
    }

    /**
     * 数学算法：计算余弦相似度
     */
    private double calculateCosineSimilarity(Map<Long, Double> usersA, Map<Long, Double> usersB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<Long, Double> entry : usersA.entrySet()) {
            Long userId = entry.getKey();
            Double scoreA = entry.getValue();
            if (usersB.containsKey(userId)) {
                dotProduct += scoreA * usersB.get(userId);
            }
            normA += Math.pow(scoreA, 2);
        }
        for (Double scoreB : usersB.values()) {
            normB += Math.pow(scoreB, 2);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}