<template>
  <div class="my-products-container">
    <el-card class="box-card">
      <!-- 统计信息 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总数</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="6">
          <div class="stat-item active">
            <div class="stat-value">{{ stats.onSale }}</div>
            <div class="stat-label">在售</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.sold }}</div>
            <div class="stat-label">已售</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.offline }}</div>
            <div class="stat-label">下架</div>
          </div>
        </el-col>
      </el-row>

      <el-divider></el-divider>

      <!-- 筛选和操作 -->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="filter-form">
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="按状态筛选" clearable @change="handleQuery">
            <el-option label="全部" value="" />
            <el-option label="在售" value="0" />
            <el-option label="已售" value="1" />
            <el-option label="下架" value="2" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">发布新商品</el-button>
          <el-button icon="el-icon-arrow-left" @click="$router.go(-1)">返回列表</el-button>
        </el-form-item>
      </el-form>

      <el-divider></el-divider>

      <!-- 商品表格 -->
      <el-table :data="productList" stripe v-loading="loading">
        <el-table-column prop="productId" label="ID" width="80" />
        <el-table-column label="商品" min-width="250">
          <template slot-scope="scope">
            <div class="product-cell">
              <img
                :src="scope.row.imageUrls ? scope.row.imageUrls.split(',')[0] : '/404.png'"
                :alt="scope.row.title"
                class="product-thumb"
              />
              <div class="product-text">
                <div class="product-title">{{ scope.row.title }}</div>
                <div class="product-desc">{{ scope.row.description | truncate(30) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="price" label="价格" width="100">
          <template slot-scope="scope">
            <span class="price">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag
              :type="
                scope.row.status === '0' ? 'success' : scope.row.status === '1' ? 'danger' : 'info'
              "
            >
              {{ scope.row.status === '0' ? '在售' : scope.row.status === '1' ? '已售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="发布时间" width="150" />

        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="goToDetail(scope.row.productId)"
            >
              查看
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row.productId)"
              v-if="scope.row.status === '0'"
            >
              编辑
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row.productId)"
            >
              删除
            </el-button>
            <el-dropdown trigger="click" v-if="scope.row.status === '0'">
              <el-button size="mini" type="text">
                更多<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click.native="handleOffline(scope.row.productId)">
                  下架
                </el-dropdown-item>
                <el-dropdown-item @click.native="handleRefresh(scope.row.productId)">
                  刷新
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

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
import { listProduct, delProduct } from '@/api/campus/product'

export default {
  name: 'MyProducts',
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
        pageSize: 10,
        status: '',
        userId: this.$store.state.user.id
      },
      stats: {
        total: 0,
        onSale: 0,
        sold: 0,
        offline: 0
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listProduct(this.queryParams)
        .then(response => {
          this.productList = response.rows || []
          this.total = response.total || 0

          // 计算统计信息
          this.stats = {
            total: response.total || 0,
            onSale: this.productList.filter(p => p.status === '0').length,
            sold: this.productList.filter(p => p.status === '1').length,
            offline: this.productList.filter(p => p.status === '2').length
          }

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
    goToDetail(productId) {
      this.$router.push({
        name: 'MarketDetail',
        query: { id: productId }
      })
    },
    handleAdd() {
      this.$router.push({ name: 'AddProduct' })
    },
    handleEdit(productId) {
      this.$router.push({
        name: 'EditProduct',
        query: { id: productId }
      })
    },
    handleDelete(productId) {
      this.$modal.confirm('是否确认删除该商品？').then(() => {
        return delProduct(productId)
      }).then(() => {
        this.$message.success('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleOffline(productId) {
      this.$message.info('下架功能开发中')
    },
    handleRefresh(productId) {
      this.$message.info('刷新功能开发中')
    }
  }
}
</script>

<style scoped lang="scss">
.my-products-container {
  padding: 20px;

  .stats-row {
    margin-bottom: 20px;

    .stat-item {
      background: #f5f7fa;
      padding: 20px;
      border-radius: 8px;
      text-align: center;
      transition: all 0.3s ease;

      &:hover {
        background: #e6f7ff;
      }

      &.active {
        background: #e6f7ff;
        border: 2px solid #1890ff;
      }

      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #333;
        margin-bottom: 10px;
      }

      .stat-label {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .filter-form {
    margin-bottom: 15px;
  }

  .product-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .product-thumb {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 4px;
    }

    .product-text {
      flex: 1;
      min-width: 0;

      .product-title {
        font-weight: 600;
        font-size: 14px;
        color: #333;
        margin-bottom: 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .product-desc {
        font-size: 12px;
        color: #999;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .price {
    color: #ff6b6b;
    font-weight: bold;
  }
}
</style>
