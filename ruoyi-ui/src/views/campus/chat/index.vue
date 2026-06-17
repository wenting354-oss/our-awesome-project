<template>
  <div class="chat-wrapper">
    <div class="session-sidebar">
      <div class="sidebar-title">消息中心</div>
      <div class="session-list">
        <div
          v-for="item in sessions"
          :key="item.receiverId"
          :class="['session-item', receiverId == item.receiverId ? 'active' : '']"
          @click="changeSession(item.receiverId)"
        >
          <el-badge :value="item.unreadCount" :max="99" :hidden="item.unreadCount === 0" class="avatar-badge">
            <el-avatar :src="item.targetAvatar || defaultAvatar" size="medium"></el-avatar>
          </el-badge>
          <div class="session-info">
            <div class="session-name">{{ item.targetName }}</div>
            <div class="session-last-msg">{{ item.lastMessage }}</div>
          </div>
        </div>
        <div v-if="sessions.length === 0" class="empty-sessions">暂无聊天简讯</div>
      </div>
    </div>

    <div class="chat-main">
      <div v-if="receiverId" class="chat-box">
        <div class="chat-header">
          <span class="active-title">正在与交易方沟通</span>
        </div>

        <div class="message-list" ref="messageList">
          <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.senderId == currentUserId ? 'me' : 'other']">
            <div class="bubble">
              {{ msg.content }}
            </div>
          </div>
        </div>

        <div class="input-area">
          <el-input
            v-model="inputMessage"
            @keyup.enter.native="sendMessage"
            placeholder="轻点回车发送消息..."
          >
            <el-button slot="append" type="primary" @click="sendMessage">发送</el-button>
          </el-input>
        </div>
      </div>
      <div v-else class="no-chat-selected">
        <i class="el-icon-chat-dot-round" style="font-size: 60px; color: #ccc;"></i>
        <p>选择左侧会话开始进行在线回复沟通吧</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getChatHistory, getChatSessions, markAsRead } from "@/api/campus/chat";

export default {
  name: "PrivateChat",
  data() {
    return {
      inputMessage: '',
      messages: [],
      sessions: [],
      currentUserId: this.$store.state.user.id,
      receiverId: null,
      defaultAvatar: require("@/assets/images/profile.jpg")
    };
  },
  created() {
    this.loadSessions();
    this.receiverId = this.$route.query.targetId;
    if (this.receiverId) {
      this.loadHistory();
    }

    // 监听全局布局接收到的即时通知总线
    this.$bus.$on('receiveGlobalMessage', (msg) => {
      // 刚好是在和这个人聊天，直接塞入屏幕
      if (msg.senderId == this.receiverId) {
        this.messages.push(msg);
        this.scrollToBottom();
        markAsRead(this.receiverId); // 顺便标为已读
      }
      this.loadSessions(); // 刷新左侧会话状态和消息预览
    });
  },
  beforeDestroy() {
    this.$bus.$off('receiveGlobalMessage');
  },
  methods: {
    loadSessions() {
      getChatSessions().then(res => {
        this.sessions = res.data;
      });
    },
    loadHistory() {
      getChatHistory(this.receiverId).then(res => {
        this.messages = res.data;
        this.scrollToBottom();
        markAsRead(this.receiverId).then(() => {
          this.loadSessions();
        });
      });
    },
    changeSession(targetId) {
      this.receiverId = targetId;
      // 优雅更换路由参数不刷页面
      this.$router.push({ path: this.$route.path, query: { targetId: targetId } }).catch(()=>{});
      this.loadHistory();
    },
    sendMessage() {
      if (!this.inputMessage.trim()) return;

      const chatMsg = {
        senderId: this.currentUserId,
        content: this.inputMessage
      };

      // 实时追加到右侧视窗显示
      this.messages.push(chatMsg);

      // 通过 layout 中初始化的全局持久 WebSocket 连接发送消息给后端
      const outMsg = {
        receiverId: this.receiverId,
        content: this.inputMessage
      };

      // 寻找挂载在最外层的全局Ws实例来发流消息
      let rootLayout = this.$parent;
      while (rootLayout && !rootLayout.globalWs) {
        rootLayout = rootLayout.$parent;
      }

      if (rootLayout && rootLayout.globalWs && rootLayout.globalWs.readyState === WebSocket.OPEN) {
        rootLayout.globalWs.send(JSON.stringify(outMsg));
      } else {
        this.$message.error("实时通讯连接已断开，请尝试刷新重试");
      }

      this.inputMessage = '';
      this.scrollToBottom();
      setTimeout(() => { this.loadSessions(); }, 300); // 顺延重新计算左方最后一条消息
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messageList;
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      });
    }
  }
};
</script>

<style scoped>
.chat-wrapper {
  display: flex;
  height: calc(100vh - 84px);
  background-color: #f0f2u5;
}
.session-sidebar {
  width: 280px;
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}
.sidebar-title {
  padding: 20px;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #f2f2f2;
}
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.3s;
}
.session-item:hover, .session-item.active {
  background-color: #f2f6fc;
}
.avatar-badge {
  margin-right: 15px;
}
.session-info {
  flex: 1;
  overflow: hidden;
}
.session-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}
.session-last-msg {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.empty-sessions {
  text-align: center;
  color: #999;
  margin-top: 40px;
  font-size: 13px;
}
.chat-main {
  flex: 1;
  background-color: #f7f9fb;
}
.chat-box {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chat-header {
  padding: 18px 20px;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}
.active-title {
  font-size: 15px;
  font-weight: bold;
}
.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
.message-item {
  display: flex;
  margin-bottom: 15px;
}
.message-item.me {
  justify-content: flex-end;
}
.message-item.me .bubble {
  background-color: #95ec69;
  color: #000;
  border-radius: 8px 0 8px 8px;
}
.message-item.other .bubble {
  background-color: #fff;
  color: #333;
  border-radius: 0 8px 8px 8px;
}
.bubble {
  padding: 11px 16px;
  max-width: 65%;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.input-area {
  padding: 15px 20px;
  background-color: #fff;
  border-top: 1px solid #eee;
}
.no-chat-selected {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #999;
}
</style>
