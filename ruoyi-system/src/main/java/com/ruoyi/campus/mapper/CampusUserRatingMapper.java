package com.ruoyi.campus.mapper;

import com.ruoyi.campus.domain.CampusUserRating;
import org.apache.ibatis.annotations.Param;

public interface CampusUserRatingMapper {
    // 新增评价
    int insertRating(CampusUserRating rating);

    // 检查订单是否已评价
    int checkOrderRated(Long orderId);

    // 计算某卖家的平均得分
    Double calculateAverageScore(Long sellerId);
}