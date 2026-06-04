<template>
  <div class="my-circle-wrapper">
    <!-- 现代化标签页 -->
    <div class="modern-tabs-container">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick" class="custom-flat-tabs">
        <el-tab-pane label="📝 我的发布" name="my-posts"></el-tab-pane>
        <el-tab-pane label="👍 我的点赞" name="my-likes"></el-tab-pane>
        <el-tab-pane label="💬 我的评论" name="my-comments"></el-tab-pane>
        <el-tab-pane label="⭐ 我的收藏" name="my-favorites"></el-tab-pane>
      </el-tabs>
    </div>

    <!-- 话题流区域 -->
    <div class="topic-feed-stream" v-loading="loading">
      <el-empty v-if="topicList.length === 0 && !loading" :description="emptyText"></el-empty>

      <!-- 现代化卡片 -->
      <div class="social-feed-card" v-for="topic in topicList" :key="topic.topicId" :class="'card-border-' + topic.topicType">
        <!-- 卡片头部 -->
        <div class="card-header-zone">
          <el-avatar class="user-avatar-glow" :src="topic.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c672f1epng.png'"></el-avatar>
          <div class="user-meta-data">
            <div class="author-nickname">{{ topic.nickName }}</div>
            <div class="post-time">⏱️ {{ parseTime(topic.createTime, '{y}-{m}-{d} {h}:{i}') }}</div>
          </div>
          <span class="modern-topic-badge" :class="'badge-' + topic.topicType">
            {{ formatTopicType(topic.topicType) }}
          </span>
        </div>

        <!-- 卡片主体内容 -->
        <div class="card-body-zone">
          <div v-if="topic.content" class="post-text-content" v-html="topic.content.replace(/\n/g, '<br>')"></div>

          <!-- 固定大小的图片流 -->
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

          <!-- 我的发布时显示编辑删除按钮 -->
          <div v-if="activeTab === 'my-posts'" class="manage-actions">
            <el-button type="text" icon="el-icon-edit" class="edit-btn" @click="handleEdit(topic)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" class="delete-btn" @click="handleDelete(topic)">删除</el-button>
          </div>
        </div>

        <!-- 评论舱 -->
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
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 编辑话题弹窗 -->
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

    <!-- 评论弹窗 -->
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
import { listMyTopic, listMyLikes, listMyComments, listMyFavorites, toggleLike, getTopic, updateTopic, delTopic, getComments, addComment, toggleFavorite } from "@/api/campus/topic";
import ImageUpload from '@/components/ImageUpload';

export default {
  name: "MyProfile",
  components: { ImageUpload },
  data() {
    return {
      loading: true,
      activeTab: 'my-posts',
      topicList: [],
      commentList: {},
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
      open: false,
      title: "",
      form: {},
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
      },
      topicTypeOptions: [
        { value: 'recommend', label: '推荐' },
        { value: 'qa', label: '问答' },
        { value: 'secondhand', label: '二手' },
        { value: 'dating', label: '恋爱交友' },
        { value: 'jobs', label: '兼职信息' },
        { value: 'gossip', label: '校园八卦' }
      ]
    };
  },
  computed: {
    emptyText() {
      switch (this.activeTab) {
        case 'my-posts': return '您还没有发布过任何话题';
        case 'my-likes': return '您还没有点赞过任何话题';
        case 'my-comments': return '您还没有评论过任何话题';
        case 'my-favorites': return '您还没有收藏过任何话题';
        default: return '暂无数据';
      }
    }
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      let apiCall;
      if (this.activeTab === 'my-posts') {
        apiCall = listMyTopic(this.queryParams);
      } else if (this.activeTab === 'my-likes') {
        apiCall = listMyLikes(this.queryParams);
      } else if (this.activeTab === 'my-comments') {
        apiCall = listMyComments(this.queryParams);
      } else if (this.activeTab === 'my-favorites') {
        apiCall = listMyFavorites(this.queryParams);
      }

      apiCall.then(response => {
        this.topicList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.topicList.forEach(topic => {
          this.handleGetComments(topic.topicId);
        });
      });
    },
    handleTabClick() {
      this.queryParams.pageNum = 1;
      this.topicList = [];
      this.getList();
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
          if (this.activeTab === 'my-likes') {
            this.topicList = this.topicList.filter(item => item.topicId !== topic.topicId);
            if(this.topicList.length === 0){
              this.getList()
            }
          }
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
          if (this.activeTab === 'my-favorites') {
            this.topicList = this.topicList.filter(item => item.topicId !== topic.topicId);
            if(this.topicList.length === 0){
              this.getList()
            }
          }
        }
      });
    },
    formatTopicType(type) {
      const option = this.topicTypeOptions.find(item => item.value === type);
      return option ? option.label : '其他';
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
    cancel() {
      this.open = false;
      this.reset();
    },
    handleEdit(topic) {
      this.reset();
      const topicId = topic.topicId;
      getTopic(topicId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "编辑话题";
      });
    },
    handleDelete(topic) {
      const topicIds = topic.topicId;
      this.$modal.confirm('是否确认删除该话题？').then(function() {
        return delTopic(topicIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.topicId != null) {
            updateTopic(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleGetComments(topicId) {
      getComments(topicId).then(response => {
        this.$set(this.commentList, topicId, response.data);
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
          }).catch(()=>{});
        }
      });
    }
  }
};
</script>

<style scoped lang="scss">
.my-circle-wrapper {
  background-color: #dee2e6;
  min-height: calc(100vh - 84px);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;

  .modern-tabs-container {
    width: 100%;
    max-width: 680px;
    background: #ffffff;
    padding: 2px 16px;
    border-radius: 30px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;

    ::v-deep .el-tabs__nav-wrap::after { display: none; }
    ::v-deep .el-tabs__item {
      font-size: 14px;
      color: #495057;
      height: 40px;
      line-height: 40px;
      &.is-active {
        font-weight: 700;
        color: #1890ff;
      }
    }
  }

  .topic-feed-stream {
    width: 100%;
    max-width: 680px;
  }

  /* 现代化社交卡片 */
  .social-feed-card {
    background: #ffffff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    border: 1px solid #ced4da;
    border-left: 5px solid #6c757d;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }

    /* 根据话题类型改变左边框颜色 */
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

      .user-avatar-glow {
        flex-shrink: 0;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .user-meta-data {
        margin-left: 12px;
        flex: 1;
        .author-nickname {
          font-size: 15px;
          font-weight: 700;
          color: #212529;
        }
        .post-time {
          font-size: 12px;
          color: #868e96;
          margin-top: 2px;
        }
      }

      .modern-topic-badge {
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 11px;
        background-color: #e9ecef;
        color: #495057;
        white-space: nowrap;
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
        word-break: break-word;
      }

      .modern-image-fixed-flex {
        margin-top: 12px;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .fixed-image-item {
          width: 110px !important;
          height: 110px !important;
          border-radius: 6px;
          cursor: pointer;
          border: 1px solid #dee2e6;
          display: inline-block;
        }
      }
    }

    .card-actions-zone {
      display: flex;
      align-items: center;
      border-top: 1px solid #f1f3f5;
      padding-top: 12px;
      gap: 0;

      .action-btn-item {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        font-size: 13px;
        color: #495057;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
          color: #1890ff;
        }
        &.liked {
          color: #1890ff;
          font-weight: bold;
        }
        &.favorited {
          color: #fa8c16;
          font-weight: bold;
        }
      }

      .manage-actions {
        margin-left: auto;
        display: flex;
        gap: 8px;

        .edit-btn {
          color: #1890ff;
          &:hover { color: #40a9ff; }
        }
        .delete-btn {
          color: #ff7875;
          &:hover { color: #ff4d4f; }
        }
      }
    }

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

        .comment-row-avatar {
          flex-shrink: 0;
        }

        .comment-row-body {
          margin-left: 8px;
          width: 100%;
          font-size: 13px;

          .comment-text-bubble {
            color: #212529;
            line-height: 1.5;
            .comment-user {
              font-weight: 600;
              color: #495057;
            }
            .reply-arrow-text {
              color: #868e96;
              margin: 0 2px;
            }
            .comment-main-content {
              color: #343a40;
            }
          }

          .comment-row-footer {
            display: flex;
            justify-content: space-between;
            margin-top: 4px;
            color: #868e96;
            font-size: 11px;

            .comment-action-reply {
              cursor: pointer;
              transition: color 0.3s ease;
              &:hover {
                color: #1890ff;
              }
            }
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
    margin-top: 20px;
    ::v-deep .pagination-container {
      background: transparent !important;
      padding: 0;
    }
  }
}

::v-deep .modern-styled-dialog {
  border-radius: 12px !important;
  .el-dialog__header {
    border-bottom: 1px solid #f0f0f0;
  }
  .el-textarea__inner {
    border-radius: 8px;
    background-color: #fbfbfd;
    border: 1px solid #d9d9d9;
    padding: 10px;
  }
  .dialog-footer {
    text-align: right;
    padding-top: 10px;
  }
}

.action-glow-btn {
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(24, 144, 255, 0.3);
  &:hover {
    box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4) !important;
  }
}
</style>
