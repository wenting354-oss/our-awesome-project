<template>
  <div class="market-container">
    <el-card class="box-card">
      <!-- 搜索和筛选区域 -->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="search-form">
        <el-form-item prop="productName">
          <el-input
            v-model="queryParams.productName"
            placeholder="搜索商品名称..."
            clearable
            @keyup.enter.native="handleQuery"
            class="search-input"
          />
        </el-form-item>

        <el-form-item prop="category">
          <el-select v-model="queryParams.category" placeholder="选择分类" clearable>
            <el-option label="全部分类" value="" />
            <el-option label="电子产品" value="1" />
            <el-option label="图书教材" value="2" />
            <el-option label="生活用品" value="3" />
            <el-option label="服装配饰" value="4" />
            <el-option label="其他" value="5" />
          </el-select>
        </el-form-item>

        <el-form-item prop="sortBy">
          <el-select v-model="queryParams.sortBy" placeholder="排序方式" clearable @change="handleQuery">
            <el-option label="最新发布" value="createTime" />
            <el-option label="价格低到高" value="priceAsc" />
            <el-option label="价格高到低" value="priceDesc" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>

        <el-form-item class="action-right">
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">发布商品</el-button>
          <el-button icon="el-icon-s-fold" @click="goToMyProducts">我的商品</el-button>
        </el-form-item>
      </el-form>

      <!-- 商品网格展示 -->
      <div class="product-grid" v-loading="loading">
        <el-empty v-if="productList.length === 0 && !loading" description="暂无商品"></el-empty>

        <div class="grid-container">
          <div
            v-for="product in productList"
            :key="product.productId"
            class="product-card"
            @click="goToDetail(product.productId)"
          >
            <div class="product-image">
              <img
                :src="product.imageUrls ? product.imageUrls.split(',')[0] : '/404.png'"
                :alt="product.title"
                class="img"
              />
              <div v-if="product.status !== '0'" class="product-badge">
                {{ product.status === '1' ? '已售' : '下架' }}
              </div>
              <div class="product-hover-actions">
                <el-button
                  type="text"
                  size="small"
                  icon="el-icon-view"
                  @click.stop="goToDetail(product.productId)"
                >
                  查看详情
                </el-button>
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-name">{{ product.title }}</h3>
              <p class="product-desc">{{ product.description | truncate(50) }}</p>

              <div class="product-meta">
                <span class="category-tag">{{ getCategoryLabel(product.category) }}</span>
              </div>

              <div class="product-footer">
                <span class="product-price">¥{{ product.price }}</span>
                <span class="product-seller">{{ product.userName || '用户' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script>
import { listProduct } from '@/api/campus/product'

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
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        productName: '',
        category: '',
        sortBy: 'createTime',
        status: '0' // 默认只查询在售商品
      },
      categoryMap: {
        '1': '电子产品',
        '2': '图书教材',
        '3': '生活用品',
        '4': '服装配饰',
        '5': '其他'
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      const query = { ...this.queryParams }
      // 处理排序参数
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

      listProduct(query)
        .then(response => {
          this.productList = response.rows || []
          this.total = response.total || 0
          this.loading = false
        })
        .catch(() => {
          this.$message.error('加载商品列表失败')
          this.loading = false
        })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = {
        pageNum: 1,
        pageSize: 12,
        productName: '',
        category: '',
        sortBy: 'createTime',
        status: '0'
      }
      this.getList()
    },
    goToDetail(productId) {
      this.$router.push({
        name: 'MarketDetail',
        query: { id: productId }
      })
    },
    handleAdd() {
      this.$router.push({ name: 'AddProduct' })
    },
    goToMyProducts() {
      this.$router.push({ name: 'MyProducts' })
    },
    getCategoryLabel(categoryId) {
      return this.categoryMap[categoryId] || '其他'
    }
  }
}
</script>

<style scoped lang="scss">
.market-container {
  padding: 20px;

  .box-card {
    border-radius: 8px;
  }

  .search-form {
    margin-bottom: 20px;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .search-input {
      width: 200px;
    }

    .action-right {
      margin-left: auto;
    }

    ::v-deep .el-form-item {
      margin-bottom: 10px;
    }
  }

  .product-grid {
    min-height: 300px;

    .grid-container {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 20px;
      margin-bottom: 20px;
    }
  }

  .product-card {
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #eee;
    transition: all 0.3s ease;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    &:hover {
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
      transform: translateY(-4px);

      .product-hover-actions {
        opacity: 1;
      }
    }

    .product-image {
      position: relative;
      width: 100%;
      padding-bottom: 100%;
      background: #f5f5f5;
      overflow: hidden;

      .img {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s ease;
      }

      &:hover .img {
        transform: scale(1.05);
      }

      .product-badge {
        position: absolute;
        top: 10px;
        right: 10px;
        background: rgba(0, 0, 0, 0.7);
        color: #fff;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: bold;
      }

      .product-hover-actions {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        background: rgba(0, 0, 0, 0.7);
        padding: 10px;
        display: flex;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s ease;

        ::v-deep .el-button {
          color: #fff;
          border-color: #fff;
        }
      }
    }

    .product-info {
      padding: 12px;

      .product-name {
        margin: 0 0 8px 0;
        font-size: 14px;
        font-weight: 600;
        color: #333;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .product-desc {
        margin: 0 0 8px 0;
        font-size: 12px;
        color: #999;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .product-meta {
        margin-bottom: 8px;

        .category-tag {
          display: inline-block;
          background: #f0f2f5;
          color: #666;
          padding: 2px 6px;
          border-radius: 3px;
          font-size: 11px;
        }
      }

      .product-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #eee;
        padding-top: 8px;

        .product-price {
          color: #ff6b6b;
          font-size: 16px;
          font-weight: bold;
        }

        .product-seller {
          font-size: 11px;
          color: #999;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .market-container .product-grid .grid-container {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px;
  }
}
</style>
