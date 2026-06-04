<template>
  <div class="my-errand-wrapper">
    <!-- 现代化标签页 -->
    <div class="modern-tabs-container">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick" class="custom-flat-tabs">
        <el-tab-pane label="📤 我发布的" name="published"></el-tab-pane>
        <el-tab-pane label="📥 我接受的" name="taken"></el-tab-pane>
      </el-tabs>
    </div>

    <!-- 任务列表 -->
    <div class="errand-grid-container" v-loading="loading">
      <el-empty v-if="orderList.length === 0 && !loading" :description="emptyText" class="empty-container"></el-empty>

      <!-- 任务卡片 -->
      <div class="errand-card-modern" v-for="order in orderList" :key="order.orderId" :class="statusClass(order.status)">
        <!-- 状态指示器 -->
        <div class="status-indicator" :class="'status-' + order.status"></div>

        <!-- 卡片头部 -->
        <div class="card-header-modern">
          <div class="type-and-reward">
            <el-tag class="task-type-tag" :class="'tag-' + getTaskColor(order.orderType)">
              {{ getTaskIcon(order.orderType) }} {{ order.orderType }}
            </el-tag>
            <div class="reward-section">
              <span class="reward-amount">¥{{ order.reward }}</span>
              <span class="reward-hint">悬赏</span>
            </div>
          </div>
          <el-tag :type="statusType(order.status)" class="status-badge">
            {{ formatStatus(order.status) }}
          </el-tag>
        </div>

        <!-- 卡片主体 -->
        <div class="card-body-modern">
          <h3 class="task-title">{{ order.title }}</h3>
          <p class="task-detail">{{ order.detail }}</p>

          <!-- 地址信息 -->
          <div class="location-section">
            <i class="el-icon-location"></i>
            <span class="location-text">{{ order.deliveryAddress }}</span>
          </div>
        </div>

        <!-- 卡片底部 -->
        <div class="card-footer-modern">
          <!-- 左侧：发布时间 -->
          <span class="publish-info">
            <i class="el-icon-time"></i>
            {{ formatPublishTime(order.createTime) }}
          </span>

          <!-- 右侧：操作按钮 -->
          <div class="action-buttons">
            <el-button
              v-if="activeTab === 'published' && order.status === '1'"
              type="success"
              size="mini"
              round
              class="complete-btn"
              icon="el-icon-check"
              @click="handleComplete(order)"
            >送达</el-button>
            <el-button
              v-if="activeTab === 'published' && order.status === '0'"
              type="danger"
              size="mini"
              round
              class="cancel-btn"
              icon="el-icon-close"
              @click="handleCancel(order)"
            >取消</el-button>
          </div>
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
  </div>
</template>

<script>
import { listMyPublished, listMyTaken, updateErrand } from "@/api/campus/errand";

export default {
  name: "MyErrands",
  data() {
    return {
      loading: true,
      activeTab: 'published',
      orderList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 12,
      },
    };
  },
  computed: {
    emptyText() {
      return this.activeTab === 'published' ? '您还没有发布过任何任务' : '您还没有接受过任何任务';
    }
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      let apiCall = this.activeTab === 'published' ? listMyPublished(this.queryParams) : listMyTaken(this.queryParams);
      apiCall.then(response => {
        this.orderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleTabClick() {
      this.queryParams.pageNum = 1;
      this.orderList = [];
      this.getList();
    },
    formatStatus(status) {
      if (status === '0') return '待接单';
      if (status === '1') return '进行中';
      if (status === '2') return '已完成';
      if (status === '3') return '已取消';
      return '未知';
    },
    statusType(status) {
      if (status === '0') return 'info';
      if (status === '1') return 'warning';
      if (status === '2') return 'success';
      if (status === '3') return 'danger';
      return 'info';
    },
    statusClass(status) {
      if (status === '2') return 'is-completed';
      if (status === '3') return 'is-cancelled';
      return '';
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
    handleComplete(order) {
      this.$modal.confirm('是否确认任务已送达？').then(() => {
        const updatedOrder = { orderId: order.orderId, status: '2' };
        return updateErrand(updatedOrder);
      }).then(() => {
        this.$modal.msgSuccess("操作成功！订单已完成。");
        this.getList();
      }).catch(() => {});
    },
    handleCancel(order) {
      this.$modal.confirm('是否确认取消这个跑腿任务？').then(() => {
        const updatedOrder = { orderId: order.orderId, status: '3' };
        return updateErrand(updatedOrder);
      }).then(() => {
        this.$modal.msgSuccess("订单已取消");
        this.getList();
      }).catch(() => {});
    }
  }
};
</script>

<style scoped lang="scss">
.my-errand-wrapper {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 84px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;

  .modern-tabs-container {
    width: 100%;
    max-width: 1000px;
    background: white;
    padding: 2px 16px;
    border-radius: 30px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    margin-bottom: 24px;

    ::v-deep .el-tabs__nav-wrap::after { display: none; }
    ::v-deep .el-tabs__item {
      font-size: 14px;
      color: #495057;
      height: 40px;
      line-height: 40px;
      &.is-active {
        font-weight: 700;
        color: #667eea;
      }
    }
  }

  .errand-grid-container {
    width: 100%;
    max-width: 1000px;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
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
    position: relative;

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
      border-color: #e0e0e0;
    }

    /* 状态类样式 */
    &.is-completed,
    &.is-cancelled {
      opacity: 0.7;

      .status-indicator {
        background: linear-gradient(135deg, #d4d4d4, #a8a8a8);
      }
    }

    .status-indicator {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      transition: background 0.3s ease;

      &.status-0 { background: linear-gradient(135deg, #1890ff, #0050b3); } // 待接单 - 蓝色
      &.status-1 { background: linear-gradient(135deg, #ffa940, #ff7a45); } // 进行中 - 橙色
      &.status-2 { background: linear-gradient(135deg, #52c41a, #389e0d); } // 已完成 - 绿色
      &.status-3 { background: linear-gradient(135deg, #ff4d4f, #cf1322); } // 已取消 - 红色
    }

    .card-header-modern {
      padding: 16px 20px 12px;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;

      .type-and-reward {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        flex: 1;

        .task-type-tag {
          border-radius: 6px;
          font-size: 12px;
          font-weight: 600;
          padding: 4px 10px;
          border: none !important;
          white-space: nowrap;

          &.tag-express { background: #ffe0e0; color: #ff6b6b; border: 1px solid #ffb3b3 !important; }
          &.tag-food { background: #fff1e6; color: #ffa940; border: 1px solid #ffc069 !important; }
          &.tag-campus { background: #f1f9e6; color: #52c41a; border: 1px solid #b7eb8f !important; }
          &.tag-other { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff !important; }
        }

        .reward-section {
          display: flex;
          flex-direction: column;
          align-items: flex-start;
          gap: 2px;

          .reward-amount {
            font-size: 18px;
            font-weight: 700;
            background: linear-gradient(135deg, #ff6b6b, #ffa940);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
          }
          .reward-hint {
            font-size: 10px;
            color: #999;
          }
        }
      }

      .status-badge {
        white-space: nowrap;
        flex-shrink: 0;
      }
    }

    .card-body-modern {
      padding: 12px 20px;
      flex: 1;

      .task-title {
        margin: 0 0 8px 0;
        font-size: 15px;
        font-weight: 700;
        color: #212529;
        line-height: 1.4;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .task-detail {
        margin: 0 0 10px 0;
        font-size: 13px;
        color: #666;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .location-section {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #999;

        i {
          color: #ffa940;
          font-weight: bold;
        }

        .location-text {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .card-footer-modern {
      padding: 12px 20px;
      border-top: 1px solid #f5f5f5;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;

      .publish-info {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #999;

        i {
          font-size: 13px;
        }
      }

      .action-buttons {
        display: flex;
        gap: 8px;

        .complete-btn,
        .cancel-btn {
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
          }
        }

        .complete-btn {
          background: linear-gradient(135deg, #52c41a, #389e0d) !important;
          border: none !important;
          color: white !important;
          box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3) !important;
        }

        .cancel-btn {
          background: linear-gradient(135deg, #ff4d4f, #cf1322) !important;
          border: none !important;
          color: white !important;
          box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3) !important;
        }
      }
    }
  }

  .pagination-container {
    width: 100%;
    max-width: 1000px;
    display: flex;
    justify-content: center;
    margin-top: 24px;

    ::v-deep .pagination-container {
      background: transparent !important;
      padding: 0;
    }
  }
}
</style>
