<template>
  <div class="errand-hall-wrapper">
    <!-- 顶部操作栏 -->
    <div class="errand-header-bar">
      <div class="header-content">
        <h2 class="hall-title">🚀 校园跑腿大厅</h2>
        <p class="hall-subtitle">发布任务或接单赚取报酬</p>
      </div>
      <el-button
        type="primary"
        icon="el-icon-plus"
        size="large"
        round
        class="publish-btn-main"
        @click="handlePublish"
      >发布新任务</el-button>
    </div>

    <!-- 搜索和筛选栏 -->
    <el-collapse-transition>
      <div v-show="showSearch" class="search-filter-panel">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" @submit.native.prevent>
          <el-form-item prop="orderType">
            <el-select v-model="queryParams.orderType" placeholder="筛选任务类型" clearable style="width: 180px">
              <el-option label="全部" value=""></el-option>
              <el-option label="快递代取" value="快递代取"></el-option>
              <el-option label="外卖代拿" value="外卖代拿"></el-option>
              <el-option label="校园跑腿" value="校园跑腿"></el-option>
              <el-option label="其他" value="其他"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" round @click="handleQuery">筛选</el-button>
            <el-button icon="el-icon-refresh" size="mini" round @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-collapse-transition>

    <!-- 任务卡片网格 -->
    <div class="errand-grid-container" v-loading="loading">
      <el-empty v-if="orderList.length === 0 && !loading" description="当前没有待接单的任务" class="empty-container"></el-empty>

      <!-- 任务卡片 -->
      <div class="errand-card-modern" v-for="order in orderList" :key="order.orderId" :class="'task-' + getTaskColor(order.orderType)">
        <!-- 卡片顶部标签和奖励 -->
        <div class="card-tag-header">
          <el-tag class="task-type-badge" :class="'badge-' + getTaskColor(order.orderType)">
            {{ getTaskIcon(order.orderType) }} {{ order.orderType }}
          </el-tag>
          <div class="reward-badge">
            <span class="reward-value">¥{{ order.reward }}</span>
            <span class="reward-label">悬赏</span>
          </div>
        </div>

        <!-- 卡片主体 -->
        <div class="card-main-content">
          <h3 class="task-title">{{ order.title }}</h3>
          <p class="task-detail">{{ order.detail }}</p>

          <!-- 地址信息 -->
          <div class="location-info">
            <i class="el-icon-location"></i>
            <span>{{ order.deliveryAddress }}</span>
          </div>
        </div>

        <!-- 发布者信息和动作按钮 -->
        <div class="card-footer-modern">
          <div class="publisher-card">
            <el-avatar
              size="36"
              :src="order.publisherAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c672f1epng.png'"
              class="publisher-avatar"
            ></el-avatar>
            <div class="publisher-details">
              <p class="publisher-name">{{ order.publisherNickName }}</p>
              <p class="publish-time">⏱️ {{ formatPublishTime(order.createTime) }}</p>
            </div>
          </div>
          <el-button
            type="success"
            size="medium"
            round
            class="take-order-btn"
            @click="handleTakeOrder(order)"
            icon="el-icon-check"
          >接单</el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container">
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 发布任务弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body custom-class="errand-dialog-modern">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="任务类型" prop="orderType">
          <el-select v-model="form.orderType" placeholder="请选择任务类型" style="width: 100%">
            <el-option label="快递代取" value="快递代取"></el-option>
            <el-option label="外卖代拿" value="外卖代拿"></el-option>
            <el-option label="校园跑腿" value="校园跑腿"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="一句话描述你的需求" />
        </el-form-item>
        <el-form-item label="详细描述" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="4"
            placeholder="例如取件码、具体商品、期望送达时间等"
            resize="none"
          />
        </el-form-item>
        <el-form-item label="送达地址" prop="deliveryAddress">
          <el-input v-model="form.deliveryAddress" placeholder="请输入宿舍楼、教室等详细地址" />
        </el-form-item>
        <el-form-item label="悬赏金额" prop="reward">
          <el-input-number
            v-model="form.reward"
            :precision="2"
            :step="1"
            :min="0"
            style="width: 100%"
          ></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button round @click="cancel">取 消</el-button>
        <el-button type="primary" round class="publish-confirm-btn" @click="submitForm">发 布</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listErrand, addErrand, takeErrandOrder } from "@/api/campus/errand";

export default {
  name: "ErrandHall",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      orderList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        status: '0',
        orderType: ''
      },
      form: {},
      rules: {
        orderType: [
          { required: true, message: "任务类型不能为空", trigger: "change" }
        ],
        title: [
          { required: true, message: "订单标题不能为空", trigger: "blur" }
        ],
        reward: [
          { required: true, message: "悬赏金额不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listErrand(this.queryParams).then(response => {
        this.orderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    formatPublishTime(createTime) {
      if (!createTime) return '未知时间';

      const now = new Date();
      const publishTime = new Date(createTime);
      const diffMs = now.getTime() - publishTime.getTime();
      const diffSeconds = Math.floor(diffMs / 1000);
      const diffMinutes = Math.floor(diffSeconds / 60);
      const diffHours = Math.floor(diffMinutes / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffSeconds < 60) {
        return '刚刚发布';
      } else if (diffMinutes < 60) {
        return `${diffMinutes}分钟前`;
      } else if (diffHours < 24) {
        return `${diffHours}小时前`;
      } else if (diffDays < 7) {
        return `${diffDays}天前`;
      } else {
        return this.parseTime(createTime, '{y}-{m}-{d}');
      }
    },
    getTaskColor(orderType) {
      const colorMap = {
        '快递代取': 'express',
        '外卖代拿': 'food',
        '校园跑腿': 'campus',
        '其他': 'other'
      };
      return colorMap[orderType] || 'other';
    },
    getTaskIcon(orderType) {
      const iconMap = {
        '快递代取': '📦',
        '外卖代拿': '🍱',
        '校园跑腿': '🏃',
        '其他': '🎯'
      };
      return iconMap[orderType] || '🎯';
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        orderId: null,
        orderType: "快递代取",
        title: null,
        detail: null,
        reward: 5,
        deliveryAddress: null,
      };
      this.resetForm("form");
    },
    handlePublish() {
      this.reset();
      this.open = true;
      this.title = "发布跑腿任务";
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addErrand(this.form).then(response => {
            this.$modal.msgSuccess("发布成功");
            this.open = false;
            this.getList();
          });
        }
      });
    },
    handleTakeOrder(row) {
      this.$modal.confirm('是否确认接收这个跑腿任务？').then(() => {
        return takeErrandOrder(row.orderId);
      }).then(() => {
        this.$modal.msgSuccess("接单成功！");
        this.getList();
      }).catch(() => {});
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.orderType = '';
      this.handleQuery();
    }
  }
};
</script>

<style scoped lang="scss">
.errand-hall-wrapper {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 84px);
  padding: 30px 20px;

  .errand-header-bar {
    max-width: 1200px;
    margin: 0 auto 30px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 40px 30px;
    border-radius: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
    color: white;

    .header-content {
      .hall-title {
        margin: 0;
        font-size: 32px;
        font-weight: 700;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }
      .hall-subtitle {
        margin: 8px 0 0 0;
        font-size: 14px;
        opacity: 0.9;
      }
    }

    .publish-btn-main {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%) !important;
      border: none !important;
      box-shadow: 0 4px 15px rgba(245, 87, 108, 0.4) !important;
      font-weight: 600;
      padding: 0 32px !important;

      &:hover {
        box-shadow: 0 6px 20px rgba(245, 87, 108, 0.5) !important;
        transform: translateY(-2px);
      }
    }
  }

  .search-filter-panel {
    max-width: 1200px;
    margin: 0 auto 20px;
    background: white;
    padding: 16px 20px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .errand-grid-container {
    max-width: 1200px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 20px;

    .empty-container {
      grid-column: 1 / -1;
      height: 400px;
    }
  }

  /* 现代化任务卡片 */
  .errand-card-modern {
    background: white;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    border: 1px solid #f0f0f0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: column;
    cursor: pointer;

    &:hover {
      transform: translateY(-8px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
      border-color: #e0e0e0;
    }

    /* 任务类型颜色主题 */
    &.task-express {
      border-top: 4px solid #ff6b6b;
      .badge-express { background: #ffe0e0; color: #ff6b6b; border: 1px solid #ffb3b3; }
    }
    &.task-food {
      border-top: 4px solid #ffa940;
      .badge-food { background: #fff1e6; color: #ffa940; border: 1px solid #ffc069; }
    }
    &.task-campus {
      border-top: 4px solid #52c41a;
      .badge-campus { background: #f1f9e6; color: #52c41a; border: 1px solid #b7eb8f; }
    }
    &.task-other {
      border-top: 4px solid #1890ff;
      .badge-other { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
    }

    .card-tag-header {
      padding: 16px 20px 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      .task-type-badge {
        border-radius: 6px;
        font-size: 12px;
        font-weight: 600;
        padding: 4px 10px;
        border: none !important;
        background: #e6f7ff;
        color: #1890ff;
      }

      .reward-badge {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 2px;

        .reward-value {
          font-size: 22px;
          font-weight: 700;
          background: linear-gradient(135deg, #f5576c, #ffa940);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }
        .reward-label {
          font-size: 11px;
          color: #999;
        }
      }
    }

    .card-main-content {
      padding: 16px 20px;
      flex: 1;

      .task-title {
        margin: 0 0 10px 0;
        font-size: 16px;
        font-weight: 700;
        color: #212529;
        line-height: 1.4;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .task-detail {
        margin: 0 0 12px 0;
        font-size: 13px;
        color: #666;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .location-info {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #999;

        i {
          color: #ffa940;
          font-weight: bold;
        }
      }
    }

    .card-footer-modern {
      padding: 16px 20px;
      border-top: 1px solid #f5f5f5;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;

      .publisher-card {
        display: flex;
        align-items: center;
        gap: 10px;
        flex: 1;
        min-width: 0;

        .publisher-avatar {
          flex-shrink: 0;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .publisher-details {
          min-width: 0;
          flex: 1;

          .publisher-name {
            margin: 0;
            font-size: 13px;
            font-weight: 600;
            color: #212529;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .publish-time {
            margin: 2px 0 0 0;
            font-size: 11px;
            color: #999;
          }
        }
      }

      .take-order-btn {
        flex-shrink: 0;
        background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%) !important;
        border: none !important;
        color: white !important;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3) !important;

        &:hover {
          box-shadow: 0 4px 12px rgba(82, 196, 26, 0.4) !important;
          transform: translateY(-2px);
        }

        &:active {
          transform: translateY(0);
        }
      }
    }
  }

  .pagination-container {
    max-width: 1200px;
    margin: 30px auto 0;
    display: flex;
    justify-content: center;

    ::v-deep .pagination-container {
      background: transparent !important;
      padding: 0;
    }
  }
}

/* 现代化弹窗样式 */
::v-deep .errand-dialog-modern {
  border-radius: 16px !important;

  .el-dialog__header {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  }

  .el-dialog__title {
    font-weight: 700;
    color: #212529;
  }

  .el-form-item {
    margin-bottom: 16px;
  }

  .el-input__inner,
  .el-textarea__inner {
    border-radius: 8px !important;
    border: 1px solid #d9d9d9 !important;
    background-color: #fafafa !important;
    transition: all 0.3s ease;

    &:focus {
      border-color: #667eea !important;
      background-color: white !important;
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1) !important;
    }
  }

  .dialog-footer {
    text-align: right;
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;
  }
}

.publish-confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(102, 126, 234, 0.3) !important;

  &:hover {
    box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4) !important;
  }
}
</style>
