<template>
  <div class="detail-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/market-page/index' }">二手商城</el-breadcrumb-item>
      <el-breadcrumb-item>商品详情</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card class="box-card" shadow="hover">
      <el-row :gutter="40" class="detail-content">
        <el-col :xs="24" :sm="10" :md="10">
          <div class="image-container">
            <el-carousel height="450px" :autoplay="false" arrow="always" trigger="click" indicator-position="outside">
              <el-carousel-item v-for="(url, index) in productImages" :key="index">
                <el-image
                  :src="url"
                  :preview-src-list="productImages"
                  fit="contain"
                  class="carousel-img">
                  <div slot="placeholder" class="image-slot">
                    <i class="el-icon-loading"></i> 加载中...
                  </div>
                </el-image>
              </el-carousel-item>
            </el-carousel>
          </div>
        </el-col>

        <el-col :xs="24" :sm="14" :md="14">
          <div class="info-container">
            <h2 class="product-title">{{ product.title }}</h2>

            <div class="price-section">
              <span class="price-symbol">¥</span>
              <span class="price">{{ product.price }}</span>
              <el-tag :type="product.status === '0' ? 'success' : 'info'" class="status-tag" effect="dark" size="small">
                {{ product.status === '0' ? '正在热卖' : product.status === '1' ? '已被抢购' : '已下架' }}
              </el-tag>
            </div>

            <div class="meta-section">
              <div class="info-item">
                <span class="label"><i class="el-icon-collection-tag"></i> 分类</span>
                <span class="value">{{ getCategoryLabel(product.category) }}</span>
              </div>
              <div class="info-item">
                <span class="label"><i class="el-icon-time"></i> 发布时间</span>
                <span class="value">{{ product.createTime }}</span>
              </div>
              <div class="info-item">
                <span class="label"><i class="el-icon-view"></i> 浏览量</span>
                <span class="value">{{ product.viewCount || 0 }} 次浏览</span>
              </div>
            </div>

            <el-divider></el-divider>

            <div class="seller-card">
              <el-avatar :size="64" :src="product.avatar || '/avatar.png'" class="seller-avatar"></el-avatar>

              <div class="seller-details">
                <template v-if="product.userName === 'admin'">
                  <div class="seller-header">
                    <span class="seller-name" style="color: #f5222d;">校方后勤服务中心</span>
                    <el-tag size="mini" type="danger" effect="dark" style="border-radius: 12px; margin-left: 8px;">
                      <i class="el-icon-s-custom"></i> 官方发布
                    </el-tag>
                  </div>
                  <div class="student-info-tags">
                    <el-tag size="mini" type="danger" plain class="custom-tag">官方自营</el-tag>
                    <el-tag size="mini" type="danger" plain class="custom-tag">品质保障</el-tag>
                  </div>
                  <div class="seller-rating" style="color: #f5222d; font-weight: bold;">
                    ⭐ 官方信誉保证
                  </div>
                </template>

                <template v-else>
                  <div class="seller-header">
                    <span class="seller-name">{{ product.nickName || '匿名同学' }}</span>
                    <el-tag size="mini" type="success" effect="dark" style="border-radius: 12px; margin-left: 8px;">
                      <i class="el-icon-circle-check"></i> 校园认证
                    </el-tag>
                  </div>

                  <div class="student-info-tags">
                    <el-tag size="mini" type="info" plain class="custom-tag">
                      <i class="el-icon-school"></i> {{ product.college || '未知学院' }}
                    </el-tag>
                    <el-tag size="mini" type="info" plain class="custom-tag">
                      <i class="el-icon-reading"></i> {{ product.major || '未知专业' }}
                    </el-tag>
                  </div>

                  <div class="seller-rating">
                    学号：{{ product.userName || '保密' }} | ⭐ 信用极好
                  </div>
                </template>
              </div>

              <div class="seller-contact-btn">
                <el-button plain round size="small" icon="el-icon-phone-outline" @click="handleContact">
                  电话/微信
                </el-button>

                <el-button type="primary" round size="small" icon="el-icon-chat-dot-round" @click="handleContactSeller(product.userId)">
                  在线私信
                </el-button>
              </div>
            </div>

            <div class="action-buttons" v-if="product.status === '0'">
              <el-button type="warning" size="medium" icon="el-icon-star-off" @click="handleCollect" :plain="!isCollected" class="collect-btn">
                {{ isCollected ? '已加入收藏' : '加入收藏' }}
              </el-button>
            </div>
            <div v-else class="sold-out-mask">
              <el-alert title="该商品当前不在售" type="warning" show-icon :closable="false"></el-alert>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="description-section">
        <h3 class="section-title"><span>商品详情</span></h3>
        <div class="description-content">
          {{ product.description }}
        </div>
      </div>

      <div class="recommend-section" v-if="recommendList.length > 0">
        <h3 class="section-title"><span style="color: #ff4d4f;">猜你喜欢</span></h3>
        <el-row :gutter="20">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in recommendList" :key="item.id" style="margin-bottom: 20px;">
            <el-card shadow="hover" class="recommend-card" @click.native="handleRecommendClick(item.productId || item.id)">
              <el-image
                class="recommend-img"
                :src="item.imageUrls ? item.imageUrls.split(',')[0] : '/404.png'"
                fit="cover">
              </el-image>
              <div class="recommend-info">
                <h4 class="recommend-title">{{ item.title }}</h4>
                <p class="recommend-price">￥ {{ item.price }}</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

    </el-card>

    <el-dialog title="卖家联系方式" :visible.sync="contactOpen" width="380px" custom-class="contact-dialog" center>
      <div class="contact-box">
        <p><i class="el-icon-user"></i> 卖家：<strong>{{ product.userName === 'admin' ? '校方后勤' : product.nickName }}</strong></p>
        <p><i class="el-icon-chat-dot-round"></i> 联系方式：<strong>{{ product.contactInfo || '卖家未留下联系方式' }}</strong></p>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="contactOpen = false">关 闭</el-button>
        <el-button type="primary" @click="handleCopy">一键复制</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getProduct, recordUserBehavior, getRecommendProducts } from '@/api/campus/product'

export default {
  name: 'ProductDetail',
  data() {
    return {
      product: {},
      productImages: [],
      isCollected: false,
      contactOpen: false,
      recommendList: [],
      categoryMap: {
        '1': '电子产品', '2': '图书教材', '3': '生活用品', '4': '服装配饰', '5': '其他'
      },
      conditionMap: {
        '1': '全新', '2': '几乎全新', '3': '九成新', '4': '八成新', '5': '七成新', '6': '较旧'
      }
    }
  },
  watch: {
    '$route.query.id': {
      handler(newId) {
        if (newId) {
          this.initData()
        }
      },
      immediate: true
    }
  },
  methods: {
    initData() {
      this.getDetail()
      this.loadRecommend()
    },
    getDetail() {
      const productId = this.$route.query.id
      if (!productId) {
        this.$message.error('商品ID不存在')
        return
      }

      getProduct(productId)
        .then(response => {
          this.product = response.data || {}
          this.productImages = this.product.imageUrls ? this.product.imageUrls.split(',') : ['/404.png']
          // 算法埋点
          recordUserBehavior(productId, 1).catch(() => {});
        })
        .catch(() => {
          this.$message.error('加载商品详情失败')
        })
    },
    loadRecommend() {
      getRecommendProducts().then(response => {
        this.recommendList = response.rows || response.data || [];
      }).catch(() => {});
    },
    handleRecommendClick(id) {
      this.$router.push({ path: '/market-page/detail', query: { id: id } })
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    handleContact() {
      this.contactOpen = true
    },
    handleCopy() {
      const sellerName = this.product.userName === 'admin' ? '校方后勤' : this.product.nickName;
      const text = `卖家：${sellerName}\n联系方式：${this.product.contactInfo || '暂无'}`;
      this.$copyText(text).then(() => {
        this.$message.success('已复制到剪贴板')
      })
    },
    handleCollect() {
      this.isCollected = !this.isCollected
      const msg = this.isCollected ? '收藏成功' : '已取消收藏'
      this.$message.success(msg)
      if (this.isCollected && this.product.id) {
        recordUserBehavior(this.product.id, 2).catch(() => {});
      }
    },
    getCategoryLabel(categoryId) {
      return this.categoryMap[categoryId] || '其他'
    },
    getConditionLabel(conditionId) {
      return this.conditionMap[conditionId] || '未知'
    },
    handleContactSeller(sellerId) {
      if (!sellerId) {
        this.$message.warning("获取卖家信息失败");
        return;
      }
      // 👈 将 path 修改为我们刚刚配置的无冲突路由
      this.$router.push({
        path: '/private-chat/index',
        query: { targetId: sellerId }
      });
    }
  }
}
</script>

<style scoped lang="scss">
.detail-container {
  padding: 24px;
  background-color: #f4f6f8; /* 让卡片在底色中凸显出来 */
  min-height: calc(100vh - 84px);

  .breadcrumb {
    margin-bottom: 20px;
    font-size: 14px;
  }

  .box-card {
    border-radius: 12px;
    border: none;
    padding: 10px;
  }

  .detail-content {
    margin-bottom: 40px;

    .image-container {
      background: #f8f9fa; /* 图片没有填满时显示淡灰色底 */
      border-radius: 12px;
      overflow: hidden;
      border: 1px solid #ebeef5;

      .carousel-img {
        width: 100%;
        height: 100%;
        background-color: #f8f9fa;
      }

      .image-slot {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        color: #909399;
        font-size: 14px;
      }
    }

    .info-container {
      padding: 10px 20px;

      .product-title {
        font-size: 26px;
        margin: 0 0 20px 0;
        font-weight: 600;
        color: #2c3e50;
        line-height: 1.4;
      }

      .price-section {
        background: #fff0f0; /* 仿淘宝价格区背景色 */
        padding: 16px 20px;
        border-radius: 8px;
        display: flex;
        align-items: baseline;
        margin-bottom: 24px;

        .price-symbol {
          color: #f5222d;
          font-size: 18px;
          font-weight: bold;
          margin-right: 4px;
        }

        .price {
          color: #f5222d;
          font-size: 36px;
          font-weight: 700;
          margin-right: 20px;
        }
      }

      .meta-section {
        background: #f8f9fa;
        padding: 20px;
        border-radius: 8px;

        .info-item {
          display: flex;
          align-items: center;
          margin-bottom: 14px;
          font-size: 14px;

          &:last-child {
            margin-bottom: 0;
          }

          .label {
            color: #909399;
            width: 100px;
            i { margin-right: 4px; }
          }

          .value {
            color: #303133;
            font-weight: 500;
          }

          .condition-value {
            color: #e6a23c;
          }
        }
      }

      /* ✅ 修复后的名片样式 */
      .seller-card {
        margin-top: 30px;
        display: flex;
        align-items: center;
        background: linear-gradient(to right, #f2f8fe, #ffffff);
        padding: 20px;
        border-radius: 16px;
        border: 1px solid #e1eaf4;
        transition: box-shadow 0.3s;

        &:hover {
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
        }

        .seller-avatar {
          border: 3px solid #fff;
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
        }

        .seller-details {
          flex: 1;
          margin-left: 16px;

          .seller-header {
            display: flex;
            align-items: center;
            margin-bottom: 8px;

            .seller-name {
              font-size: 18px;
              font-weight: bold;
              color: #2c3e50;
            }
          }

          .student-info-tags {
            margin-bottom: 8px;
            display: flex;
            gap: 6px;
            flex-wrap: wrap;

            .custom-tag {
              border-radius: 4px;
              background: #fff;
            }
          }

          .seller-rating {
            font-size: 12px;
            color: #909399;
          }
        }
      }

      .action-buttons {
        margin-top: 30px;
        .collect-btn {
          width: 180px;
          font-size: 16px;
          border-radius: 24px;
        }
      }

      .sold-out-mask {
        margin-top: 30px;
      }
    }
  }

  .section-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
    padding-left: 12px;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 18px;
      background-color: #409EFF;
      border-radius: 2px;
    }
  }

  .description-section {
    margin-top: 40px;

    .description-content {
      padding: 24px;
      border-radius: 8px;
      line-height: 1.8;
      white-space: pre-wrap;
      color: #606266;
      font-size: 15px;
      border: 1px dashed #dcdfe6;
    }
  }

  .recommend-section {
    margin-top: 60px;

    .section-title::before {
      background-color: #ff4d4f;
    }

    .recommend-card {
      cursor: pointer;
      border-radius: 8px;
      border: none;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-6px);
        box-shadow: 0 8px 24px rgba(0,0,0,0.1) !important;
      }

      .recommend-img {
        width: 100%;
        height: 180px;
        background-color: #f5f5f5;
      }

      .recommend-info {
        padding: 14px;
        .recommend-title {
          margin: 0 0 10px 0;
          font-size: 15px;
          color: #303133;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        .recommend-price {
          color: #f5222d;
          font-weight: bold;
          font-size: 16px;
          margin: 0;
        }
      }
    }
  }
}

.contact-box {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  font-size: 15px;
  line-height: 2;

  p { margin: 10px 0; color: #606266; }
  strong { color: #303133; margin-left: 8px; }
  i { font-size: 16px; color: #409EFF; }
}
</style>
