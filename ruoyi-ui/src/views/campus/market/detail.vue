<template>
  <div class="detail-container">
    <el-card class="box-card">
      <el-button @click="goBack" icon="el-icon-arrow-left" type="text">返回列表</el-button>

      <el-row :gutter="30" class="detail-content">
        <!-- 左侧图片区 -->
        <el-col :xs="24" :sm="12" :md="12">
          <div class="image-container">
            <el-carousel :autoplay="true" arrow="always" indicator-position="outside">
              <el-carousel-item v-for="(url, index) in productImages" :key="index">
                <img :src="url" :alt="`商品图${index + 1}`" class="carousel-img" />
              </el-carousel-item>
            </el-carousel>
          </div>
        </el-col>

        <!-- 右侧信息区 -->
        <el-col :xs="24" :sm="12" :md="12">
          <div class="info-container">
            <h2 class="product-title">{{ product.title }}</h2>

            <div class="price-section">
              <span class="price">¥{{ product.price }}</span>
              <el-tag :type="product.status === '0' ? 'success' : 'danger'" class="status-tag">
                {{ product.status === '0' ? '在售' : product.status === '1' ? '已售' : '下架' }}
              </el-tag>
            </div>

            <el-divider></el-divider>

            <div class="info-item">
              <span class="label">分类：</span>
              <span>{{ getCategoryLabel(product.category) }}</span>
            </div>

            <div class="info-item">
              <span class="label">新旧程度：</span>
              <span>{{ getConditionLabel(product.condition) }}</span>
            </div>

            <div class="info-item">
              <span class="label">发布者：</span>
              <span>{{ product.userName || '用户' }}</span>
            </div>

            <div class="info-item">
              <span class="label">发布时间：</span>
              <span>{{ product.createTime }}</span>
            </div>

            <div class="info-item">
              <span class="label">浏览：</span>
              <span>{{ product.viewCount || 0 }} 次</span>
            </div>

            <el-divider></el-divider>

            <!-- 操作按钮 -->
            <div class="action-buttons" v-if="product.status === '0'">
              <el-button type="primary" size="large" icon="el-icon-phone" @click="handleContact">
                联系卖家
              </el-button>
              <el-button size="large" icon="el-icon-star-off" @click="handleCollect">
                {{ isCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>

            <div class="seller-info">
              <h4>卖家信息</h4>
              <div class="seller-card">
                <el-avatar :size="50" :src="product.sellerAvatar"></el-avatar>
                <div class="seller-details">
                  <div class="seller-name">{{ product.userName || '用户' }}</div>
                  <div class="seller-rating">⭐ {{ product.sellerRating || '暂无' }} 好评率</div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 商品描述 -->
      <el-divider></el-divider>
      <div class="description-section">
        <h3>商品描述</h3>
        <div class="description-content">
          {{ product.description }}
        </div>
      </div>
    </el-card>

    <!-- 联系卖家对话框 -->
    <el-dialog title="联系卖家" :visible.sync="contactOpen" width="400px">
      <p>卖家信息：<strong>{{ product.userName }}</strong></p>
      <p>电话：<strong>{{ product.sellerPhone || '暂未提供' }}</strong></p>
      <p>微信：<strong>{{ product.sellerWeChat || '暂未提供' }}</strong></p>
      <p>QQ：<strong>{{ product.sellerQQ || '暂未提供' }}</strong></p>
      <div slot="footer" class="dialog-footer">
        <el-button @click="contactOpen = false">关闭</el-button>
        <el-button type="primary" @click="handleCopy">复制联系信息</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getProduct } from '@/api/campus/product'

export default {
  name: 'ProductDetail',
  data() {
    return {
      product: {},
      productImages: [],
      isCollected: false,
      contactOpen: false,
      categoryMap: {
        '1': '电子产品',
        '2': '图书教材',
        '3': '生活用品',
        '4': '服装配饰',
        '5': '其他'
      },
      conditionMap: {
        '1': '全新',
        '2': '几乎全新',
        '3': '九成新',
        '4': '八成新',
        '5': '七成新',
        '6': '较旧'
      }
    }
  },
  created() {
    this.getDetail()
  },
  methods: {
    getDetail() {
      const productId = this.$route.query.id
      if (!productId) {
        this.$message.error('商品ID不存在')
        this.goBack()
        return
      }

      getProduct(productId)
        .then(response => {
          this.product = response.data || {}
          this.productImages = this.product.imageUrls ? this.product.imageUrls.split(',') : ['/404.png']
          // 这里可以调用API增加浏览次数
        })
        .catch(() => {
          this.$message.error('加载商品详情失败')
        })
    },
    goBack() {
      this.$router.go(-1)
    },
    handleContact() {
      this.contactOpen = true
    },
    handleCopy() {
      const text = `卖家：${this.product.userName}\n电话：${this.product.sellerPhone || '暂未提供'}\n微信：${
        this.product.sellerWeChat || '暂未提供'
      }\nQQ：${this.product.sellerQQ || '暂未提供'}`
      this.$copyText(text)
        .then(() => {
          this.$message.success('已复制到剪贴板')
        })
        .catch(() => {
          this.$message.error('复制失败')
        })
    },
    handleCollect() {
      this.isCollected = !this.isCollected
      const msg = this.isCollected ? '收藏成功' : '已取消收藏'
      this.$message.success(msg)
    },
    getCategoryLabel(categoryId) {
      return this.categoryMap[categoryId] || '其他'
    },
    getConditionLabel(conditionId) {
      return this.conditionMap[conditionId] || '未知'
    }
  }
}
</script>

<style scoped lang="scss">
.detail-container {
  padding: 20px;

  .detail-content {
    margin-top: 20px;

    .image-container {
      width: 100%;
      background: #f5f5f5;
      border-radius: 8px;
      overflow: hidden;

      ::v-deep .el-carousel {
        .el-carousel__item {
          background: #f5f5f5;
          display: flex;
          justify-content: center;
          align-items: center;
        }
      }

      .carousel-img {
        width: 100%;
        height: auto;
        max-height: 500px;
        object-fit: contain;
      }
    }

    .info-container {
      .product-title {
        font-size: 24px;
        margin: 0 0 20px 0;
        font-weight: bold;
        color: #333;
      }

      .price-section {
        display: flex;
        align-items: center;
        gap: 20px;
        margin-bottom: 20px;

        .price {
          color: #ff6b6b;
          font-size: 28px;
          font-weight: bold;
        }

        .status-tag {
          font-size: 14px;
        }
      }

      .info-item {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        font-size: 14px;

        .label {
          font-weight: 500;
          color: #666;
          width: 80px;
        }

        span:not(.label) {
          color: #333;
        }
      }

      .action-buttons {
        display: flex;
        gap: 10px;
        margin: 20px 0;

        .el-button {
          flex: 1;
        }
      }

      .seller-info {
        margin-top: 20px;

        h4 {
          margin: 0 0 15px 0;
          font-size: 14px;
          font-weight: 600;
        }

        .seller-card {
          background: #f5f5f5;
          padding: 15px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          gap: 15px;

          .seller-details {
            flex: 1;

            .seller-name {
              font-size: 14px;
              font-weight: 600;
              color: #333;
              margin-bottom: 4px;
            }

            .seller-rating {
              font-size: 12px;
              color: #999;
            }
          }
        }
      }
    }
  }

  .description-section {
    margin-top: 20px;

    h3 {
      margin: 0 0 15px 0;
      font-size: 16px;
      font-weight: 600;
    }

    .description-content {
      background: #f5f5f5;
      padding: 15px;
      border-radius: 8px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-wrap: break-word;
      color: #666;
    }
  }
}

@media (max-width: 768px) {
  .detail-container .detail-content {
    .image-container {
      margin-bottom: 20px;
    }
  }
}
</style>
