<template>
  <div class="app-container">
    <h1 style="color: red; font-size: 60px; text-align: center; z-index: 9999; position: relative;">
      🔥🔥 正在修改此文件 🔥🔥
    </h1>
    <el-row type="flex" justify="center">
      <el-col :xs="24" :sm="20" :md="16" :lg="12">

        <el-card class="publish-card" shadow="hover">
          <div class="publish-header">
            <h3>✨ 有什么新鲜事想告诉大家？</h3>
          </div>
          <el-input
            type="textarea"
            :rows="3"
            placeholder="分享校园生活、吐槽、求助..."
            v-model="queryParams.content"
            style="margin-bottom: 15px;"
          ></el-input>
          <div class="publish-actions">
            <el-button type="primary" icon="el-icon-position" round @click="handleAdd">发布动态</el-button>
            <el-button icon="el-icon-search" round @click="handleQuery">搜索</el-button>
          </div>
        </el-card>

        <div class="feed-list" v-loading="loading">
          <el-empty v-if="topicList.length === 0" description="暂无校园动态，快来发第一条吧！"></el-empty>

          <el-card class="topic-card" shadow="hover" v-for="(item, index) in topicList" :key="index">
            <div class="topic-header">
              <el-avatar :size="45" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png"></el-avatar>
              <div class="user-info">
                <span class="user-name">{{ item.createBy || '匿名校友' }}</span>
                <span class="publish-time">{{ parseTime(item.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
              </div>
              <div class="right-actions">
                <el-dropdown trigger="click">
                  <span class="el-dropdown-link"><i class="el-icon-more el-icon--right"></i></span>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item icon="el-icon-edit" @click.native="handleUpdate(item)">编辑</el-dropdown-item>
                    <el-dropdown-item icon="el-icon-delete" @click.native="handleDelete(item)" style="color: #F56C6C;">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </div>
            </div>

            <div class="topic-content">
              {{ item.content }}
            </div>

            <div class="topic-media" v-if="index % 2 === 0">
              <div class="mock-image"></div>
            </div>

            <div class="topic-footer">
              <div class="action-btn">
                <i class="el-icon-thumb"></i> <span>点赞 ({{ Math.floor(Math.random() * 50) + 1 }})</span>
              </div>
              <div class="action-btn">
                <i class="el-icon-chat-dot-square"></i> <span>评论 ({{ Math.floor(Math.random() * 20) }})</span>
              </div>
              <div class="action-btn">
                <i class="el-icon-share"></i> <span>分享</span>
              </div>
            </div>
          </el-card>
        </div>

        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
          class="custom-pagination"
        />

      </el-col>
    </el-row>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="帖子标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入帖子标题" />
        </el-form-item>
        <el-form-item label="帖子内容" prop="content">
          <el-input v-model="form.content" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="请输入标签，逗号分隔" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in statusOptions"
              :key="dict.dictValue"
              :label="parseInt(dict.dictValue)"
            >{{dict.dictLabel}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTopic, getTopic, delTopic, addTopic, updateTopic } from "@/api/campus/topic";

export default {
  name: "Topic",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 圈子帖子表格数据
      topicList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 状态数据字典
      statusOptions: [
        { dictValue: 0, dictLabel: "待审核" },
        { dictValue: 1, dictLabel: "已发布" },
        { dictValue: 2, dictLabel: "已拒绝" },
        { dictValue: 3, dictLabel: "用户删除" }
      ],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        content: null,
        tags: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        title: [
          { required: true, message: "帖子标题不能为空", trigger: "blur" }
        ],
        content: [
          { required: true, message: "帖子内容不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询圈子帖子列表 */
    getList() {
      this.loading = true;
      listTopic(this.queryParams).then(response => {
        this.topicList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 状态翻译
    formatStatus(status) {
      const option = this.statusOptions.find(item => item.dictValue == status);
      return option ? option.dictLabel : '未知';
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        topicId: null,
        title: null,
        content: null,
        userId: null,
        nickName: null,
        tags: null,
        status: 0,
        createTime: null,
        updateTime: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.topicId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加圈子帖子";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const topicId = row.topicId || this.ids
      getTopic(topicId).then(response => {
        this.form = response.data;
        // 确保状态为数字类型
        this.form.status = parseInt(this.form.status);
        this.open = true;
        this.title = "修改圈子帖子";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.topicId != null) {
            updateTopic(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addTopic(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const topicIds = row.topicId || this.ids;
      this.$modal.confirm('是否确认删除圈子帖子编号为"' + topicIds + '"的数据项？').then(function() {
        return delTopic(topicIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('campus/topic/export', {
        ...this.queryParams
      }, `topic_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style lang="scss" scoped>
.campus-circle-container {
  padding: 20px;
  background-color: #f4f6f9; /* 模仿微信朋友圈的浅灰背景 */
  min-height: calc(100vh - 84px);
}

.publish-card {
  border-radius: 12px;
  margin-bottom: 20px;
  border: none;
  .publish-header h3 {
    margin-top: 0;
    color: #303133;
    font-weight: 600;
  }
  .publish-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.topic-card {
  border-radius: 12px;
  margin-bottom: 15px;
  border: none;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(0,0,0,0.08) !important;
  }

  .topic-header {
    display: flex;
    align-items: center;
    margin-bottom: 15px;

    .user-info {
      margin-left: 12px;
      flex: 1;
      display: flex;
      flex-direction: column;

      .user-name {
        font-size: 16px;
        font-weight: 600;
        color: #2F54EB; /* 高级社交蓝 */
      }
      .publish-time {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }

    .el-dropdown-link {
      cursor: pointer;
      color: #909399;
      font-size: 18px;
    }
  }

  .topic-content {
    font-size: 15px;
    color: #303133;
    line-height: 1.6;
    margin-bottom: 15px;
    white-space: pre-wrap;
  }

  .topic-media {
    margin-bottom: 15px;
    .mock-image {
      width: 100%;
      height: 200px;
      background: linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%);
      border-radius: 8px;
    }
  }

  .topic-footer {
    display: flex;
    border-top: 1px solid #ebeef5;
    padding-top: 12px;

    .action-btn {
      flex: 1;
      text-align: center;
      color: #606266;
      font-size: 14px;
      cursor: pointer;
      transition: color 0.2s;

      &:hover {
        color: #2F54EB;
      }

      i {
        font-size: 16px;
        margin-right: 4px;
      }
    }
  }
}

.custom-pagination {
  text-align: center;
  margin-top: 20px;
  background: transparent !important;
}
</style>
