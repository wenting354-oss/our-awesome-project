<template>
  <div class="order-detail-container">
    <el-card class="box-card">
      <el-button @click="goBack" icon="el-icon-arrow-left" type="text">返回列表</el-button>

      <el-divider></el-divider>

      <div v-loading="loading">
        <!-- 订单基本信息 -->
        <div class="order-header">
          <div class="order-title">
            <h2>订单详情</h2>
            <el-tag :type="getStatusType(order.status)">
              {{ getStatusLabel(order.status) }}
            </el-tag>
          </div>
          <div class="order-number">
            订单号：<span>{{ order.orderId }}</span>
          </div>
        </div>

        <el-divider></el-divider>

        <!-- 商品信息 -->
        <div class="section">
          <h3 class="section-title">商品信息</h3>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="6">
              <img
                :src="order.productImage"
                :alt="order.productName"
                class="product-image"
              />
            </el-col>
            <el-col :xs="24" :sm="18">
              <div class="product-details">
                <div class="detail-item">
                  <span class="label">商品名称：</span>
                  <span>{{ order.productName }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">单价：</span>
                  <span class="price">¥{{ order.price }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">数量：</span>
                  <span>{{ order.quantity || 1 }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">商品分类：</span>
                  <span>{{ order.category }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <el-divider></el-divider>

        <!-- 价格信息 -->
        <div class="section">
          <h3 class="section-title">价格信息</h3>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <div class="price-item">
                <span class="label">商品总价：</span>
                <span class="value">¥{{ order.subtotal }}</span>
              </div>
              <div class="price-item">
                <span class="label">运费：</span>
                <span class="value">¥{{ order.shippingFee || 0 }}</span>
              </div>
              <div class="price-item">
                <span class="label">优惠：</span>
                <span class="value">-¥{{ order.discount || 0 }}</span>
              </div>
            </el-col>
            <el-col :xs="24" :sm="12">
              <div class="price-item total">
                <span class="label">应付金额：</span>
                <span class="value">¥{{ order.amount }}</span>
              </div>
              <div class="price-item">
                <span class="label">支付方式：</span>
                <span class="value">{{ order.paymentMethod || '在线支付' }}</span>
              </div>
              <div class="price-item">
                <span class="label">支付状态：</span>
                <span class="value">
                  <el-tag type="success" v-if="order.payStatus === '1'">已支付</el-tag>
                  <el-tag type="warning" v-else>待支付</el-tag>
                </span>
              </div>
            </el-col>
          </el-row>
        </div>

        <el-divider></el-divider>

        <!-- 配送信息 -->
        <div class="section">
          <h3 class="section-title">配送信息</h3>
          <div class="delivery-info">
            <div class="detail-item">
              <span class="label">收货人：</span>
              <span>{{ order.buyerName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">联系电话：</span>
              <span>{{ order.buyerPhone }}</span>
            </div>
            <div class="detail-item">
              <span class="label">收货地址：</span>
              <span>{{ order.deliveryAddress }}</span>
            </div>
            <div class="detail-item">
              <span class="label">发货时间：</span>
              <span>{{ order.shipTime || '待发货' }}</span>
            </div>
            <div class="detail-item" v-if="order.trackingNumber">
              <span class="label">物流单号：</span>
              <span>{{ order.trackingNumber }}</span>
            </div>
            <div class="detail-item" v-if="order.logistics">
              <span class="label">物流公司：</span>
              <span>{{ order.logistics }}</span>
            </div>
          </div>
        </div>

        <el-divider></el-divider>

        <!-- 时间线 -->
        <div class="section">
          <h3 class="section-title">订单时间线</h3>
          <el-timeline>
            <el-timeline-item
              timestamp="下单时间"
              placement="top"
              type="primary"
            >
              <p>{{ order.createTime }}</p>
            </el-timeline-item>
            <el-timeline-item
              timestamp="支付时间"
              placement="top"
              type="success"
              v-if="order.payTime"
            >
              <p>{{ order.payTime }}</p>
            </el-timeline-item>
            <el-timeline-item
              timestamp="发货时间"
              placement="top"
              v-if="order.shipTime"
            >
              <p>{{ order.shipTime }}</p>
            </el-timeline-item>
            <el-timeline-item
              timestamp="收货时间"
              placement="top"
              type="success"
              v-if="order.receiveTime"
            >
              <p>{{ order.receiveTime }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>

        <el-divider></el-divider>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button
            type="primary"
            @click="handleConfirm"
            v-if="order.status === '1'"
          >
            确认收货
          </el-button>
          <el-button
            @click="handleContact"
          >
            联系卖家
          </el-button>
          <el-button
            @click="goBack"
          >
            返回列表
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'OrderDetail',
  data() {
    return {
      loading: false,
      order: {
        orderId: '2025060400001',
        productName: '二手iPhone 13',
        productImage: '/img/product.jpg',
        price: 3999,
        quantity: 1,
        subtotal: 3999,
        shippingFee: 0,
        discount: 0,
        amount: 3999,
        status: '1',
        payStatus: '1',
        paymentMethod: '支付宝',
        category: '电子产品',
        buyerName: '李明',
        buyerPhone: '13800138000',
        deliveryAddress: '北京市朝阳区某某街道1号',
        createTime: '2025-06-04 10:30:00',
        payTime: '2025-06-04 10:32:00',
        shipTime: '2025-06-05 09:00:00',
        trackingNumber: 'SF1234567890',
        logistics: '顺丰速运'
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
    this.getDetail()
  },
  methods: {
    getDetail() {
      const orderId = this.$route.query.id
      if (!orderId) {
        this.$message.error('订单ID不存在')
        this.goBack()
        return
      }

      this.loading = true
      // 模拟API调用
      setTimeout(() => {
        // 这里应该调用后端API获取订单详情
        // const response = await getOrderDetail(orderId)
        // this.order = response.data
        this.loading = false
      }, 500)
    },
    goBack() {
      this.$router.go(-1)
    },
    handleConfirm() {
      this.$modal.confirm('确认已收货？').then(() => {
        this.$message.success('已确认收货')
        this.goBack()
      }).catch(() => {})
    },
    handleContact() {
      this.$message.info('联系卖家功能开发中')
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
.order-detail-container {
  padding: 20px;

  .box-card {
    border-radius: 8px;
  }

  .order-header {
    margin-bottom: 20px;

    .order-title {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 15px;

      h2 {
        margin: 0;
        font-size: 20px;
      }

      ::v-deep .el-tag {
        font-size: 14px;
      }
    }

    .order-number {
      font-size: 14px;
      color: #666;

      span {
        font-weight: bold;
        color: #333;
      }
    }
  }

  .section {
    margin-bottom: 20px;

    .section-title {
      margin: 0 0 15px 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }

    .product-image {
      width: 100%;
      max-width: 200px;
      height: auto;
      border-radius: 4px;
    }

    .product-details,
    .delivery-info {
      .detail-item {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        font-size: 14px;

        .label {
          font-weight: 500;
          color: #666;
          width: 100px;
          flex-shrink: 0;
        }

        .price {
          color: #ff6b6b;
          font-weight: bold;
          font-size: 16px;
        }
      }
    }
  }

  .price-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #eee;
    font-size: 14px;

    .label {
      color: #666;
      font-weight: 500;
    }

    .value {
      color: #333;
      font-weight: 500;
    }

    &.total {
      border-bottom: 2px solid #1890ff;
      padding: 15px 0;
      font-size: 16px;

      .label {
        font-weight: bold;
      }

      .value {
        color: #ff6b6b;
        font-size: 18px;
        font-weight: bold;
      }
    }
  }

  .action-buttons {
    text-align: center;
    padding-top: 20px;

    .el-button {
      margin: 0 10px;
    }
  }
}

@media (max-width: 768px) {
  .order-detail-container .section .product-image {
    max-width: 150px;
  }
}
</style>
