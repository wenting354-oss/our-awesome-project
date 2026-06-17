<template>
  <div class="market-container">
    <div class="market-header">
      <h2 class="page-title"><i class="el-icon-shopping-bag-1"></i> 校园二手商城</h2>
      <p class="page-subtitle">闲置物品焕发新生，校园交易安全便捷</p>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :model="queryParams" ref="queryForm" size="medium" :inline="true" class="search-form">
        <el-form-item prop="title">
          <el-input
            v-model="queryParams.title"
            placeholder="搜索你想要的宝贝..."
            clearable
            prefix-icon="el-icon-search"
            @keyup.enter.native="handleQuery"
            class="search-input"
          />
        </el-form-item>

        <el-form-item prop="category">
          <el-select v-model="queryParams.category" placeholder="全部分类" clearable @change="handleQuery" class="filter-select">
            <el-option label="全部分类" value="" />
            <el-option label="电子产品" value="1" />
            <el-option label="图书教材" value="2" />
            <el-option label="生活用品" value="3" />
            <el-option label="服装配饰" value="4" />
            <el-option label="其他" value="5" />
          </el-select>
        </el-form-item>

        <el-form-item prop="sortBy">
          <el-select v-model="queryParams.sortBy" placeholder="智能排序" clearable @change="handleQuery" class="filter-select">
            <el-option label="最新发布" value="createTime" />
            <el-option label="价格低到高" value="priceAsc" />
            <el-option label="价格高到低" value="priceDesc" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" round icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button round icon="el-icon-refresh-right" @click="resetQuery">重置</el-button>
        </el-form-item>

        <el-form-item class="action-right">
          <el-button type="warning" round icon="el-icon-magic-stick" @click="openRecommend" class="magic-btn">猜你喜欢</el-button>
          <el-button type="success" round icon="el-icon-plus" @click="handleAdd">发布闲置</el-button>
          <el-button round icon="el-icon-user" @click="goToMyProducts">我的发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="product-wrapper" v-loading="loading">
      <el-empty v-if="productList.length === 0 && !loading" description="暂时没有找到相关宝贝哦~" :image-size="200"></el-empty>

      <div class="grid-container">
        <div
          v-for="product in productList"
          :key="product.productId"
          class="product-card"
          @click="goToDetail(product.productId || product.id)"
        >
          <div class="product-image">
            <img
              :src="product.imageUrls ? product.imageUrls.split(',')[0] : '/404.png'"
              :alt="product.title"
              class="img"
            />
            <div v-if="product.status !== '0'" class="product-badge">
              {{ product.status === '1' ? '已售出' : '已下架' }}
            </div>
            <div class="product-hover-actions">
              <span class="hover-text"><i class="el-icon-view"></i> 查看详情</span>
            </div>
          </div>

          <div class="product-info">
            <h3 class="product-name" :title="product.title">{{ product.title }}</h3>
            <p class="product-desc">{{ product.description | truncate(40) }}</p>

            <div class="product-meta">
              <span class="category-tag">{{ getCategoryLabel(product.category) }}</span>
            </div>

            <div class="product-footer">
              <div class="price-wrap">
                <span class="price-symbol">¥</span>
                <span class="product-price">{{ product.price }}</span>
              </div>
              <div class="seller-wrap">
                <i class="el-icon-user-solid"></i>
                <span class="product-seller">{{ product.nickName || product.userName || '匿名同学' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-wrapper" v-show="total > 0">
      <pagination
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <el-dialog
      title="✨ 智能算法 · 猜你喜欢"
      :visible.sync="recommendOpen"
      width="1050px"
      append-to-body
      custom-class="recommend-dialog"
      top="5vh"
    >
      <div v-loading="recommendLoading" class="recommend-wrapper">
        <el-empty
          v-if="recommendList.length === 0 && !recommendLoading"
          description="暂无推荐数据，快去商城多逛逛吧~">
        </el-empty>

        <div class="grid-container" v-else>
          <div
            v-for="product in recommendList"
            :key="'rec-' + (product.productId || product.id)"
            class="product-card"
            @click="goToDetailFromRecommend(product.productId || product.id)"
          >
            <div class="product-image">
              <img
                :src="product.imageUrls ? product.imageUrls.split(',')[0] : '/404.png'"
                :alt="product.title"
                class="img"
              />
              <div v-if="product.status !== '0'" class="product-badge">
                {{ product.status === '1' ? '已售出' : '已下架' }}
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-name">{{ product.title }}</h3>
              <div class="product-meta">
                <span class="category-tag">{{ getCategoryLabel(product.category) }}</span>
              </div>
              <div class="product-footer">
                <div class="price-wrap">
                  <span class="price-symbol">¥</span>
                  <span class="product-price">{{ product.price }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProduct, getRecommendProducts } from '@/api/campus/product'

export default {
  name: 'Market',
  filters: {
    truncate(value, length) {
      if (!value) return ''
      value = value.toString()
      return value.length > length ? value.substring(0, length) + '...' : value
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      recommendOpen: false,
      recommendLoading: false,
      recommendList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        title: '',
        category: '',
        sortBy: 'createTime',
        status: '0'
      },
      categoryMap: {
        '1': '电子产品', '2': '图书教材', '3': '生活用品', '4': '服装配饰', '5': '其他'
      }
    }
  },
  created() {
    this.getList()
    this.$bus.$on('refreshMarketList', () => {
      this.getList(); // 收到信号后，立刻重新向后端请求最新数据
    });
  },
  beforeDestroy() {
    this.$bus.$off('refreshMarketList');
  },
  methods: {
    getList() {
      this.loading = true
      const query = { ...this.queryParams }
      if (query.sortBy === 'priceAsc') {
        query.orderByColumn = 'price'
        query.isAsc = 'asc'
        delete query.sortBy
      } else if (query.sortBy === 'priceDesc') {
        query.orderByColumn = 'price'
        query.isAsc = 'desc'
        delete query.sortBy
      } else {
        query.orderByColumn = 'create_time'
        query.isAsc = 'desc'
        delete query.sortBy
      }

      listProduct(query).then(response => {
        this.productList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    openRecommend() {
      this.recommendOpen = true
      this.recommendLoading = true
      getRecommendProducts().then(response => {
        this.recommendList = response.rows || response.data || [];
        this.recommendLoading = false
      }).catch(() => {
        this.recommendLoading = false
      })
    },
    goToDetailFromRecommend(productId) {
      this.recommendOpen = false
      this.goToDetail(productId)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = { pageNum: 1, pageSize: 12, title: '', category: '', sortBy: 'createTime', status: '0' }
      this.getList()
    },
    goToDetail(productId) {
      this.$router.push({ path: '/market-page/detail', query: { id: productId } })
    },
    handleAdd() {
      this.$router.push({ path: '/market-page/add' })
    },
    goToMyProducts() {
      this.$router.push({ path: '/market-page/my-products' })
    },
    getCategoryLabel(categoryId) {
      return this.categoryMap[categoryId] || '其他'
    }
  }
}
</script>

<style scoped lang="scss">
.market-container {
  padding: 24px;
  background-color: #f4f6f9; /* 浅灰蓝色背景，衬托白色卡片 */
  min-height: calc(100vh - 84px);

  .market-header {
    margin-bottom: 24px;
    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #2c3e50;
      i { color: #409EFF; margin-right: 6px; }
    }
    .page-subtitle {
      margin: 8px 0 0 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .filter-card {
    border-radius: 12px;
    border: none;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);

    .search-form {
      display: flex;
      flex-wrap: wrap;
      align-items: center;

      ::v-deep .el-form-item {
        margin-bottom: 0;
        margin-right: 16px;
      }

      .search-input {
        width: 240px;
        ::v-deep .el-input__inner {
          border-radius: 20px;
          background-color: #f5f7fa;
          border: 1px solid transparent;
          &:focus { background-color: #fff; border-color: #409EFF; }
        }
      }

      .filter-select {
        width: 140px;
        ::v-deep .el-input__inner { border-radius: 20px; }
      }

      .action-right {
        margin-left: auto;
        margin-right: 0;
        .magic-btn {
          background: linear-gradient(90deg, #ff9a9e 0%, #fecfef 99%, #fecfef 100%);
          border: none;
          color: #d11a2a;
          font-weight: bold;
          &:hover { filter: brightness(1.05); transform: translateY(-1px); }
        }
      }
    }
  }

  .product-wrapper {
    min-height: 400px;
  }

  .grid-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 24px;
  }

  /* 核心：卡片样式重构 */
  .product-card {
    background: #fff;
    border-radius: 16px;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    border: 1px solid #f0f0f0;

    &:hover {
      box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
      transform: translateY(-6px);

      .product-hover-actions {
        opacity: 1;
      }
      .img {
        transform: scale(1.05);
      }
    }

    .product-image {
      position: relative;
      width: 100%;
      padding-bottom: 100%; /* 1:1 正方形占位 */
      background: #fafafa;
      overflow: hidden;

      .img {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.4s ease;
      }

      .product-badge {
        position: absolute;
        top: 12px;
        left: 12px;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(4px);
        color: #fff;
        padding: 4px 10px;
        border-radius: 12px;
        font-size: 12px;
        letter-spacing: 1px;
      }

      .product-hover-actions {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s ease;

        .hover-text {
          background: rgba(255, 255, 255, 0.9);
          color: #333;
          padding: 8px 20px;
          border-radius: 20px;
          font-size: 14px;
          font-weight: 500;
          box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
      }
    }

    .product-info {
      padding: 16px;

      .product-name {
        margin: 0 0 6px 0;
        font-size: 15px;
        font-weight: 600;
        color: #303133;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .product-desc {
        margin: 0 0 12px 0;
        font-size: 13px;
        color: #909399;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        line-height: 1.4;
      }

      .product-meta {
        margin-bottom: 12px;
        .category-tag {
          display: inline-block;
          background: #eef2f9;
          color: #409EFF;
          padding: 2px 10px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: 500;
        }
      }

      .product-footer {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        padding-top: 12px;
        border-top: 1px dashed #ebeef5;

        .price-wrap {
          color: #ff4d4f;
          .price-symbol { font-size: 12px; font-weight: bold; margin-right: 2px; }
          .product-price { font-size: 20px; font-weight: bold; line-height: 1; }
        }

        .seller-wrap {
          display: flex;
          align-items: center;
          color: #909399;
          font-size: 12px;
          i { margin-right: 4px; font-size: 14px; }
          .product-seller {
            max-width: 80px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }

  .pagination-wrapper {
    margin-top: 30px;
    display: flex;
    justify-content: center;
    background: transparent;
  }
}

/* 推荐弹窗专属样式 */
::v-deep .recommend-dialog {
  border-radius: 16px;
  overflow: hidden;

  .el-dialog__header {
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    padding: 20px 24px;
    .el-dialog__title { font-weight: bold; color: #303133; }
  }

  .el-dialog__body {
    padding: 24px;
    background-color: #f4f6f9;
  }

  .recommend-wrapper {
    min-height: 300px;
  }
}

@media (max-width: 768px) {
  .market-container .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
}
</style>
