<template>
  <div class="my-products-container">
    <el-card class="box-card">
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总数</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.onSale }}</div>
            <div class="stat-label">在售</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="6">
          <div class="stat-item" style="border-color: #ff4d4f;">
            <div class="stat-value">{{ stats.sold }}</div>
            <div class="stat-label">已售</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="6">
          <div class="stat-item" style="border-color: #909399;">
            <div class="stat-value">{{ stats.offline }}</div>
            <div class="stat-label">下架</div>
          </div>
        </el-col>
      </el-row>

      <el-divider></el-divider>

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="filter-form">
        <el-form-item prop="category">
          <el-select v-model="queryParams.category" placeholder="按分类筛选" clearable @change="handleQuery">
            <el-option label="全部种类" value="" />
            <el-option label="电子产品" value="1" />
            <el-option label="图书教材" value="2" />
            <el-option label="生活用品" value="3" />
            <el-option label="服装配饰" value="4" />
            <el-option label="其他" value="5" />
          </el-select>
        </el-form-item>

        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="按状态筛选" clearable @change="handleQuery">
            <el-option label="商品状态" value="" />
            <el-option label="在售" value="0" />
            <el-option label="已售" value="1" />
            <el-option label="下架" value="2" />
          </el-select>
        </el-form-item>

        <el-form-item prop="sortBy">
          <el-select v-model="queryParams.sortBy" placeholder="排序方式" clearable @change="handleQuery">
            <el-option label="最新发布" value="createTimeDesc" />
            <el-option label="ID 由低到高" value="idAsc" />
            <el-option label="价格由低到高" value="priceAsc" />
            <el-option label="价格由高到低" value="priceDesc" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">发布新商品</el-button>
          <el-button icon="el-icon-arrow-left" @click="$router.go(-1)">返回列表</el-button>
        </el-form-item>
      </el-form>

      <el-divider></el-divider>

      <el-table :data="productList" stripe v-loading="loading">
        <el-table-column label="ID" width="80">
          <template slot-scope="scope">
            {{ scope.row.productId || scope.row.id }}
          </template>
        </el-table-column>

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

        <el-table-column label="分类" width="100">
          <template slot-scope="scope">
            {{ getCategoryLabel(scope.row.category) }}
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
              @click="goToDetail(scope.row.productId || scope.row.id)"
            >
              查看
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row.productId || scope.row.id)"
              v-if="scope.row.status === '0'"
            >
              编辑
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row.productId || scope.row.id)"
            >
              删除
            </el-button>
            <el-dropdown trigger="click" v-if="scope.row.status === '0'" style="margin-left: 10px;">
              <el-button size="mini" type="text">
                更多<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click.native="handleOffline(scope.row)">
                  下架
                </el-dropdown-item>
                <el-dropdown-item @click.native="handleSold(scope.row)">
                  标为已售
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

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
// ✅ 引入了 updateProduct 来实现真正的状态更新（下架/已售）
import { listProduct, delProduct, updateProduct } from '@/api/campus/product'

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
        category: '', // 新增分类筛选参数
        sortBy: 'createTimeDesc', // 新增排序参数
        userId: this.$store.state.user.id
      },
      categoryMap: {
        '1': '电子产品', '2': '图书教材', '3': '生活用品', '4': '服装配饰', '5': '其他'
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

      // ✅ 处理排序逻辑
      const query = { ...this.queryParams }
      if (query.sortBy === 'idAsc') {
        query.orderByColumn = 'product_id' // 假设你的数据库主键叫 product_id，如果是 id 请改成 id
        query.isAsc = 'asc'
      } else if (query.sortBy === 'priceAsc') {
        query.orderByColumn = 'price'
        query.isAsc = 'asc'
      } else if (query.sortBy === 'priceDesc') {
        query.orderByColumn = 'price'
        query.isAsc = 'desc'
      } else {
        query.orderByColumn = 'create_time'
        query.isAsc = 'desc'
      }
      delete query.sortBy

      listProduct(query)
        .then(response => {
          this.productList = response.rows || []
          this.total = response.total || 0

          // 计算本页统计信息
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
    getCategoryLabel(categoryId) {
      return this.categoryMap[categoryId] || '其他'
    },
    // ✅ 修复了路由跳转白屏的问题，统一使用 -page 绝对路径
    goToDetail(productId) {
      this.$router.push({ path: '/market-page/detail', query: { id: productId } })
    },
    handleAdd() {
      this.$router.push({ path: '/market-page/add' })
    },
    handleEdit(productId) {
      this.$router.push({ path: '/market-page/edit', query: { id: productId } })
    },
    handleDelete(productId) {
      this.$modal.confirm('是否确认删除该商品？').then(() => {
        return delProduct(productId)
      }).then(() => {
        this.$message.success('删除成功')
        this.getList()
      }).catch(() => {})
    },
    // ✅ 真正实现下架功能
    handleOffline(row) {
      this.$modal.confirm('确认要下架商品《' + row.title + '》吗？').then(() => {
        // 调用修改接口，把状态改成 2
        return updateProduct({ productId: row.productId || row.id, status: '2' })
      }).then(() => {
        this.$message.success('已成功下架')
        this.getList()
      }).catch(() => {})
    },
    // ✅ 顺便加了个标为已售功能
    handleSold(row) {
      this.$modal.confirm('确认该商品已售出吗？').then(() => {
        // 调用修改接口，把状态改成 1
        return updateProduct({ productId: row.productId || row.id, status: '1' })
      }).then(() => {
        this.$message.success('恭喜，商品已售出')
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
/* 样式保留你的原版不动 */
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
