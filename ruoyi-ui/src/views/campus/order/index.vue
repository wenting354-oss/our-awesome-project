<template>
  <div class="order-container">
    <el-card class="box-card">
      <!-- 筛选 -->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="filter-form">
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="按状态筛选" clearable @change="handleQuery">
            <el-option label="全部" value="" />
            <el-option label="待发货" value="0" />
            <el-option label="已发货" value="1" />
            <el-option label="已收货" value="2" />
            <el-option label="已完成" value="3" />
            <el-option label="已取消" value="4" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider></el-divider>

      <!-- 订单表格 -->
      <el-table :data="orderList" stripe v-loading="loading">
        <el-table-column prop="orderId" label="订单号" width="150" />

        <el-table-column label="商品信息" min-width="250">
          <template slot-scope="scope">
            <div class="product-info">
              <img
                :src="scope.row.productImage"
                :alt="scope.row.productName"
                class="product-image"
              />
              <div>
                <div class="product-name">{{ scope.row.productName }}</div>
                <div class="product-price">¥{{ scope.row.price }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="amount" label="金额" width="100">
          <template slot-scope="scope">
            <span class="amount">¥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="下单时间" width="150" />

        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleViewDetail(scope.row.orderId)"
            >
              详情
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="handleConfirm(scope.row.orderId)"
              v-if="scope.row.status === '1'"
            >
              确认收货
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleCancel(scope.row.orderId)"
              v-if="scope.row.status === '0'"
            >
              取消
            </el-button>
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
export default {
  name: 'Order',
  data() {
    return {
      loading: false,
      orderList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        status: ''
      },
      statusMap: {
        '0': '待发货',
        '1': '已发货',
        '2': '已收货',
        '3': '已完成',
        '4': '已取消'
      },
      statusTypeMap: {
        '0': 'warning',
        '1': 'info',
        '2': 'success',
        '3': 'success',
        '4': 'danger'
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      // 模拟数据加载
      setTimeout(() => {
        this.orderList = [
          {
            orderId: '2025060400001',
            productName: '二手iPhone 13',
            productImage: '/img/product.jpg',
            price: 3999,
            amount: 3999,
            status: '1',
            createTime: '2025-06-04 10:30:00'
          },
          {
            orderId: '2025060400002',
            productName: '编程书籍套装',
            productImage: '/img/product.jpg',
            price: 89,
            amount: 89,
            status: '3',
            createTime: '2025-06-03 15:20:00'
          }
        ]
        this.total = this.orderList.length
        this.loading = false
      }, 500)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.getList()
    },
    handleViewDetail(orderId) {
      this.$router.push({
        name: 'OrderDetail',
        query: { id: orderId }
      })
    },
    handleConfirm(orderId) {
      this.$modal.confirm('确认已收货？').then(() => {
        this.$message.success('已确认收货')
        this.getList()
      }).catch(() => {})
    },
    handleCancel(orderId) {
      this.$modal.confirm('确认取消订单？').then(() => {
        this.$message.success('订单已取消')
        this.getList()
      }).catch(() => {})
    },
    getStatusLabel(status) {
      return this.statusMap[status] || '未知'
    },
    getStatusType(status) {
      return this.statusTypeMap[status] || 'info'
    }
  }
}
</script>

<style scoped lang="scss">
.order-container {
  padding: 20px;

  .filter-form {
    margin-bottom: 15px;
  }

  .product-info {
    display: flex;
    align-items: center;
    gap: 10px;

    .product-image {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 4px;
    }

    .product-name {
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 4px;
    }

    .product-price {
      font-size: 12px;
      color: #999;
    }
  }

  .amount {
    color: #ff6b6b;
    font-weight: bold;
  }
}
</style>
