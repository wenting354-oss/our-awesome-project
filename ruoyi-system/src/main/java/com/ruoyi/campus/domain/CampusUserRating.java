package com.ruoyi.campus.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

public class CampusUserRating extends BaseEntity{
    private Long ratingId;
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private Integer score;
    private String comment;
    private Date createTime;

    public Long getRatingId() {
        return ratingId;
    }
    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
    }
    public Long getOrderId() {
        return orderId;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getBuyerId() {
        return buyerId;
    }
    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }
    public Long getSellerId() {
        return sellerId;
    }
    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
    public Integer getScore() {
        return score;
    }
    public void setScore(Integer score) {
        this.score = score;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public Date getCreateTime() {
        return createTime;
    }
}
