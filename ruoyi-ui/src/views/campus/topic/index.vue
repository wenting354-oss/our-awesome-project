<template>
  <div class="app-container campus-community-wrapper">
    <!-- 顶部扁平化流线标签页 -->
    <div class="modern-tabs-box">
      <el-tabs v-model="queryParams.topicType" @tab-click="handleQuery" class="custom-flat-tabs">
        <el-tab-pane label="🌐 全部" name="all"></el-tab-pane>
        <el-tab-pane label="🔥 推荐" name="recommend"></el-tab-pane>
        <el-tab-pane label="🙋 问答" name="qa"></el-tab-pane>
        <el-tab-pane label="🛍️ 二手" name="secondhand"></el-tab-pane>
        <el-tab-pane label="💖 恋爱交友" name="dating"></el-tab-pane>
        <el-tab-pane label="💼 兼职信息" name="jobs"></el-tab-pane>
        <el-tab-pane label="📢 校园八卦" name="gossip"></el-tab-pane>
      </el-tabs>
    </div>

    <!-- 轻量化搜索栏 -->
    <el-collapse-transition>
      <div v-show="showSearch" class="search-clean-panel">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" @submit.native.prevent>
          <el-form-item prop="content">
            <el-input
              v-model="queryParams.content"
              placeholder="搜索帖子内容或作者昵称..."
              clearable
              class="search-input-round"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" round class="action-glow-btn" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" round @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-collapse-transition>

    <!-- 工具按钮功能区 -->
    <div class="action-bar-row mb8">
      <div class="left-actions">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="medium"
          round
          class="publish-gradient-btn"
          @click="handleAdd"
          v-hasPermi="['campus:topic:add']"
        >发布新话题</el-button>
      </div>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" class="custom-toolbar"></right-toolbar>
    </div>

    <!-- 社交动态卡片 Feed 流区域 -->
    <div class="topic-feed-stream" v-loading="loading">
      <el-empty v-if="topicList.length === 0" description="暂无动态"></el-empty>

      <!-- 每一张卡片都有独立的彩色左边条，彻底拉开分区 -->
      <div class="social-feed-card" v-for="topic in topicList" :key="topic.topicId" :class="'card-border-' + topic.topicType">
        <!-- 卡片头部 -->
        <div class="card-header-zone">
          <el-avatar class="user-avatar-glow" :src="topic.avatar ? topic.avatar : 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c672f1epng.png'"></el-avatar>
          <div class="user-meta-data">
            <div class="author-nickname">{{ topic.nickName }}</div>
            <div class="post-time">⏱️ {{ parseTime(topic.createTime, '{y}-{m}-{d} {h}:{i}') }}</div>
          </div>
          <span class="modern-topic-badge" :class="'badge-' + topic.topicType">
            {{ formatTopicType(topic.topicType) }}
          </span>
        </div>

        <!-- 卡片主体 -->
        <div class="card-body-zone">
          <div v-if="topic.content" class="post-text-content" v-html="topic.content.replace(/\n/g, '<br>')"></div>

          <!-- 爆改：固定大小的多图/单图流，绝不拉伸 -->
          <div class="modern-image-fixed-flex" v-if="topic.imageUrls">
            <el-image
              v-for="(url, index) in topic.imageUrls.split(',')"
              :key="index"
              :src="url"
              :preview-src-list="topic.imageUrls.split(',')"
              class="fixed-image-item"
              fit="cover"
              lazy
            ></el-image>
          </div>
        </div>

        <!-- 卡片底部动作条 -->
        <div class="card-actions-zone">
          <div class="action-btn-item" @click="handleLike(topic)" :class="{ 'liked': topic.liked }">
            <i class="el-icon-thumb"></i> <span>点赞 ({{ topic.likeCount }})</span>
          </div>
          <div class="action-btn-item" v-if="topic.commentEnabled == '0'" @click="handleShowComment(topic.topicId, 0, '')">
            <i class="el-icon-chat-dot-round"></i> <span>评论</span>
          </div>
          <div class="action-btn-item" @click="handleFavorite(topic)" :class="{ 'favorited': topic.favorited }">
            <i class="el-icon-star-off"></i> <span>收藏</span>
          </div>
        </div>

        <!-- 二级评论舱 -->
        <div class="modern-comment-box" v-if="topic.commentEnabled == '0' && commentList[topic.topicId] && commentList[topic.topicId].length > 0">
          <div class="comment-item-row" v-for="comment in commentList[topic.topicId]" :key="comment.commentId">
            <el-avatar class="comment-row-avatar" :size="28" :src="comment.avatar ? comment.avatar : 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c672f1epng.png'"></el-avatar>
            <div class="comment-row-body">
              <div class="comment-text-bubble">
                <span class="comment-user">{{ comment.nickName }}</span>
                <span v-if="comment.parentId !== 0 && comment.replyToNickName">
                  <span class="reply-arrow-text"> 回复 </span>
                  <span class="comment-nickname">{{ comment.replyToNickName }}</span>
                </span>:
                <span class="comment-main-content">{{ comment.content }}</span>
              </div>
              <div class="comment-row-footer">
                <span class="comment-date-text">{{ parseTime(comment.createTime, '{m}-{d} {h}:{i}') }}</span>
                <span class="comment-action-reply" @click="handleShowComment(topic.topicId, comment.commentId, comment.nickName)">回复</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper-box">
      <pagination
        v-show="total>0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 发布的弹窗（原版保持不动） -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body custom-class="modern-styled-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="选择分区" prop="topicType">
          <el-select v-model="form.topicType" placeholder="请选择分区" style="width: 100%">
            <el-option
              v-for="item in topicTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="话题内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="分享新鲜事..." resize="none" />
        </el-form-item>
        <el-form-item label="上传图片" prop="imageUrls">
          <image-upload v-model="form.imageUrls"/>
        </el-form-item>
        <el-form-item label="评论设置">
          <el-radio-group v-model="form.commentEnabled">
            <el-radio label="0">允许评论</el-radio>
            <el-radio label="1">禁止评论</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button round @click="cancel">取 消</el-button>
        <el-button type="primary" round class="action-glow-btn" @click="submitForm">发 布</el-button>
      </div>
    </el-dialog>

    <!-- 评论的弹窗（原版保持不动） -->
    <el-dialog :title="commentForm.title" :visible.sync="commentOpen" width="500px" append-to-body custom-class="modern-styled-dialog">
      <el-form ref="commentForm" :model="commentForm" :rules="commentRules" label-width="0px">
        <el-form-item prop="content">
          <el-input v-model="commentForm.content" type="textarea" :rows="3" :placeholder="commentForm.placeholder" resize="none"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button round @click="cancelComment">取 消</el-button>
        <el-button type="primary" round class="action-glow-btn" @click="submitCommentForm">发 表</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTopic, addTopic, getComments, toggleLike, addComment, toggleFavorite } from "@/api/campus/topic";
import ImageUpload from '@/components/ImageUpload';

export default {
  name: "Topic",
  components: { ImageUpload },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      topicList: [],
      commentList: {},
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        topicType: 'all',
        content: null,
      },
      form: {},
      topicTypeOptions: [
        { value: 'recommend', label: '推荐' },
        { value: 'qa', label: '问答' },
        { value: 'secondhand', label: '二手' },
        { value: 'dating', label: '恋爱交友' },
        { value: 'jobs', label: '兼职信息' },
        { value: 'gossip', label: '校园八卦' }
      ],
      rules: {
        topicType: [ { required: true, message: "请选择一个分区", trigger: "change" } ],
        content: [ { required: true, message: "话题内容不能为空", trigger: "blur" } ],
      },
      commentOpen: false,
      commentForm: {
        title: '',
        placeholder: '',
        topicId: null,
        parentId: null,
        content: ''
      },
      commentRules: {
        content: [ { required: true, message: "评论内容不能为空", trigger: "blur" } ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      const params = { ...this.queryParams };
      if (params.topicType === 'all') {
        params.topicType = null;
      }
      listTopic(params).then(response => {
        this.topicList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.topicList.forEach(topic => {
          this.handleGetComments(topic.topicId);
        });
      });
    },
    handleGetComments(topicId) {
      getComments(topicId).then(response => {
        this.$set(this.commentList, topicId, response.data);
      });
    },
    handleLike(topic) {
      toggleLike(topic.topicId).then(response => {
        topic.liked = response.data;
        if (topic.liked) {
          topic.likeCount++;
          this.$modal.msgSuccess("点赞成功");
        } else {
          topic.likeCount--;
          this.$modal.msgSuccess("取消点赞");
        }
      });
    },
    handleFavorite(topic) {
      toggleFavorite(topic.topicId).then(response => {
        topic.favorited = response.data;
        if (topic.favorited) {
          this.$modal.msgSuccess("收藏成功");
        } else {
          this.$modal.msgSuccess("取消收藏");
        }
      });
    },
    formatTopicType(type) {
      const option = this.topicTypeOptions.find(item => item.value === type);
      return option ? option.label : '其他';
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        topicId: null,
        content: null,
        imageUrls: null,
        topicType: 'recommend',
        commentEnabled: '0'
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.topicType = 'all';
      this.handleQuery();
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "发布新话题";
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addTopic(this.form).then(response => {
            this.$modal.msgSuccess("发布成功");
            this.open = false;
            this.getList();
          });
        }
      });
    },
    resetCommentForm() {
      this.commentForm = {
        title: '',
        placeholder: '',
        topicId: null,
        parentId: null,
        content: ''
      };
      this.resetForm("commentForm");
    },
    handleShowComment(topicId, parentId, parentNickName) {
      this.resetCommentForm();
      this.commentForm.topicId = topicId;
      this.commentForm.parentId = parentId;
      if (parentId === 0) {
        this.commentForm.title = "发表评论";
        this.commentForm.placeholder = "请输入评论内容...";
      } else {
        this.commentForm.title = "回复 " + parentNickName;
        this.commentForm.placeholder = "回复 " + parentNickName + ":";
      }
      this.commentOpen = true;
    },
    cancelComment() {
      this.commentOpen = false;
      this.resetCommentForm();
    },
    submitCommentForm() {
      this.$refs["commentForm"].validate(valid => {
        if (valid) {
          addComment(this.commentForm).then(response => {
            this.$modal.msgSuccess("发表成功");
            this.commentOpen = false;
            this.handleGetComments(this.commentForm.topicId);
          }).catch(() => {
          });
        }
      });
    }
  }
};
</script>

<style scoped lang="scss">
.campus-community-wrapper {
  background-color: #dee2e6; /* 核心：加深背景色，让白色卡片具有强烈的视觉隔离感 */
  min-height: calc(100vh - 84px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;

  .modern-tabs-box {
    width: 100%;
    max-width: 680px;
    background: #ffffff;
    padding: 2px 16px;
    border-radius: 30px;
    box-shadow: 0 4px 16px rgba(0,0,0,0.08);
    margin-bottom: 20px;
    ::v-deep .el-tabs__nav-wrap::after { display: none; }
    ::v-deep .el-tabs__item {
      font-size: 14px;
      color: #495057;
      height: 40px;
      line-height: 40px;
      &.is-active { font-weight: 700; color: #1890ff; }
    }
  }

  .search-clean-panel {
    width: 100%;
    max-width: 680px;
    margin-bottom: 10px;
    text-align: left;
    .search-input-round ::v-deep .el-input__inner {
      width: 260px;
      border-radius: 20px !important;
    }
  }

  .action-bar-row {
    width: 100%;
    max-width: 680px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    .publish-gradient-btn {
      background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%) !important;
      border: none !important;
      color: white !important;
      font-weight: 600;
      box-shadow: 0 4px 10px rgba(24, 144, 255, 0.3);
    }
  }

  .topic-feed-stream {
    width: 100%;
    max-width: 680px;
  }

  /* 独立社交卡片 */
  .social-feed-card {
    background: #ffffff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px; /* 增大卡片间距，确保彻底区分 */
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    border: 1px solid #ced4da;
    border-left: 5px solid #6c757d; /* 默认左边框 */
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }

    /* 核心：根据不同的话题分区，赋予截然不同的左侧高亮条，让话题之间一眼就能区分！ */
    &.card-border-recommend { border-left-color: #ff4d4f; }
    &.card-border-qa { border-left-color: #1890ff; }
    &.card-border-secondhand { border-left-color: #fa8c16; }
    &.card-border-dating { border-left-color: #ff85c0; }
    &.card-border-jobs { border-left-color: #52c41a; }
    &.card-border-gossip { border-left-color: #722ed1; }

    .card-header-zone {
      display: flex;
      align-items: center;
      position: relative;
      margin-bottom: 14px;
      .user-meta-data {
        margin-left: 12px;
        .author-nickname { font-size: 15px; font-weight: 700; color: #212529; }
        .post-time { font-size: 12px; color: #868e96; margin-top: 2px; }
      }
      .modern-topic-badge {
        position: absolute;
        right: 0;
        top: 2px;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 11px;
        background-color: #e9ecef;
        color: #495057;
        &.badge-recommend { background-color: #fff1f0; color: #ff4d4f; border: 1px solid #ffccc7; }
        &.badge-qa { background-color: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
        &.badge-secondhand { background-color: #fff7e6; color: #fa8c16; border: 1px solid #ffd591; }
        &.badge-dating { background-color: #fff0f6; color: #eb2f96; border: 1px solid #ffadd2; }
        &.badge-jobs { background-color: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
        &.badge-gossip { background-color: #f9f0ff; color: #722ed1; border: 1px solid #d3adf7; }
      }
    }

    .card-body-zone {
      margin-bottom: 14px;
      .post-text-content {
        font-size: 14.5px;
        color: #343a40;
        line-height: 1.6;
      }

      /* 核心修复：单图/多图一律死死固定大小，以不拉伸的 flex 布局排开 */
      .modern-image-fixed-flex {
        margin-top: 12px;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .fixed-image-item {
          width: 110px !important; /* 强制固定宽度 */
          height: 110px !important; /* 强制固定高度 */
          border-radius: 6px;
          cursor: pointer;
          border: 1px solid #dee2e6;
          display: inline-block;
        }
      }
    }

    .card-actions-zone {
      display: flex;
      border-top: 1px solid #f1f3f5;
      padding-top: 12px;
      .action-btn-item {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        font-size: 13px;
        color: #495057;
        cursor: pointer;
        &:hover { color: #1890ff; }
        &.liked { color: #1890ff; font-weight: bold; }
        &.favorited { color: #fa8c16; font-weight: bold; }
      }
    }

    /* 内嵌评论舱 */
    .modern-comment-box {
      margin-top: 14px;
      background-color: #f8f9fa;
      border-radius: 8px;
      padding: 12px;
      border: 1px solid #e9ecef;
      .comment-item-row {
        display: flex;
        margin-bottom: 10px;
        &:last-child { margin-bottom: 0; }
        .comment-row-body {
          margin-left: 8px;
          width: 100%;
          font-size: 13px;
          .comment-text-bubble {
            color: #212529;
            .comment-user { font-weight: 600; color: #495057; }
            .reply-arrow-text { color: #868e96; margin: 0 2px; }
          }
          .comment-row-footer {
            display: flex;
            justify-content: space-between;
            margin-top: 2px;
            color: #868e96;
            font-size: 11px;
            .comment-action-reply { cursor: pointer; &:hover { color: #1890ff; } }
          }
        }
      }
    }
  }

  .pagination-wrapper-box {
    width: 100%;
    max-width: 680px;
    display: flex;
    justify-content: center;
    ::v-deep .pagination-container { background: transparent !important; padding: 0; }
  }
}

::v-deep .modern-styled-dialog {
  border-radius: 12px !important;
  .el-textarea__inner {
    border-radius: 8px;
    background-color: #fbfbfd;
    padding: 10px;
  }
}
</style>
