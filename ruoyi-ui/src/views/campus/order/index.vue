<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" style="margin-bottom: 20px;">
      <el-tab-pane label="我买到的 (买家视角)" name="buy">
        <span slot="label"><i class="el-icon-shopping-cart-full"></i> 我买到的</span>
      </el-tab-pane>
      <el-tab-pane label="我卖出的 (卖家视角)" name="sell">
        <span slot="label"><i class="el-icon-sell"></i> 我卖出的</span>
      </el-tab-pane>
    </el-tabs>

    <el-table v-loading="loading" :data="orderList" stripe border>
      <el-table-column label="订单编号" align="center" prop="orderId" width="100" />
      <el-table-column label="交易金额(元)" align="center" prop="totalAmount" width="120">
        <template slot-scope="scope">
          <span style="color: #f5222d; font-weight: bold;">￥{{ scope.row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收货地址" align="center" prop="address" show-overflow-tooltip />
      <el-table-column label="交易状态" align="center" prop="status" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" align="center" prop="createTime" width="160" />

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <template v-if="activeTab === 'buy'">
            <el-button v-if="scope.row.status === '1'" size="mini" type="danger" plain @click="handleCancel(scope.row)">取消订单</el-button>
            <el-button v-if="scope.row.status === '2'" size="mini" type="success" @click="handleConfirm(scope.row)">确认收货</el-button>
            <el-button v-if="scope.row.status === '3'" size="mini" type="primary" @click="handleRate(scope.row)">评价卖家</el-button>
          </template>

          <template v-if="activeTab === 'sell'">
            <el-button v-if="scope.row.status === '1'" size="mini" type="warning" @click="handleShip(scope.row)">立即发货</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="评价卖家" :visible.sync="rateOpen" width="400px" append-to-body>
      <el-form ref="rateForm" :model="rateForm" :rules="rateRules" label-width="80px">
        <el-form-item label="信用打分" prop="score">
          <el-rate v-model="rateForm.score" show-text :texts="['极差', '失望', '一般', '满意', '非常满意']" style="margin-top: 8px;"></el-rate>
        </el-form-item>
        <el-form-item label="评价内容" prop="comment">
          <el-input type="textarea" v-model="rateForm.comment" placeholder="请输入对卖家的评价..." rows="3"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="rateOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitRate">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMyOrders, listSoldOrders, shipOrder, confirmReceipt, cancelOrder, rateOrder } from "@/api/campus/order";

export default {
  name: "CampusOrder",
  data() {
    return {
      loading: true,
      activeTab: "buy", // 默认显示"我买到的"
      orderList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
      rateOpen: false,
      rateForm: { orderId: null, score: 5, comment: '' },
      rateRules: { score: [{ required: true, message: "请打分", trigger: "blur" }] }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      if (this.activeTab === "buy") {
        listMyOrders(this.queryParams).then(res => {
          this.orderList = res.rows;
          this.total = res.total;
          this.loading = false;
        });
      } else {
        listSoldOrders(this.queryParams).then(res => {
          this.orderList = res.rows;
          this.total = res.total;
          this.loading = false;
        });
      }
    },
    handleTabClick(tab) {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    getStatusText(status) {
      const map = { '1': '待发货', '2': '待收货', '3': '已完成', '4': '已取消' };
      return map[status] || '未知状态';
    },
    getStatusType(status) {
      const map = { '1': 'warning', '2': 'primary', '3': 'success', '4': 'info' };
      return map[status] || '';
    },
    // 发货
    handleShip(row) {
      this.$confirm('确认已将商品交给买家了吗?', '提示', { type: 'warning' }).then(() => {
        return shipOrder(row.orderId);
      }).then(() => {
        this.$message.success("发货成功");
        this.getList();
      }).catch(() => {});
    },
    // 确认收货
    handleConfirm(row) {
      this.$confirm('确认已经收到商品且无误吗?', '提示', { type: 'warning' }).then(() => {
        return confirmReceipt(row.orderId);
      }).then(() => {
        this.$message.success("收货成功");
        this.getList();
        this.handleRate(row); // 引导评价
      }).catch(() => {});
    },
    // 取消订单
    handleCancel(row) {
      this.$confirm('确定要取消该订单吗?', '提示', { type: 'warning' }).then(() => {
        return cancelOrder(row.orderId);
      }).then(() => {
        this.$message.success("取消成功");
        this.getList();
      }).catch(() => {});
    },
    // 打开评价弹窗
    handleRate(row) {
      this.rateForm = { orderId: row.orderId, score: 5, comment: '' };
      this.rateOpen = true;
    },
    submitRate() {
      this.$refs["rateForm"].validate(valid => {
        if (valid) {
          rateOrder(this.rateForm.orderId, { score: this.rateForm.score, comment: this.rateForm.comment }).then(res => {
            this.$message.success("评价成功！");
            this.rateOpen = false;
            this.getList();
          });
        }
      });
    }
  }
};
</script>
