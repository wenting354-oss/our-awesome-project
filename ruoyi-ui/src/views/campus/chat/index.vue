<template>
  <div class="chat-container">
    <div class="chat-header">
      <el-page-header @back="goBack" :content="'正在与卖家沟通'"></el-page-header>
    </div>

    <div class="message-list" ref="messageList">
      <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.isMine ? 'me' : 'other']">
        <div class="bubble">
          {{ msg.content }}
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input
        v-model="inputMessage"
        @keyup.enter.native="sendMessage"
        placeholder="发送消息..."
      >
        <el-button slot="append" @click="sendMessage">发送</el-button>
      </el-input>
    </div>
  </div>
</template>

<script>
export default {
  name: "UserChat",
  data() {
    return {
      ws: null,
      inputMessage: '',
      messages: [],
      // 假设从 Vuex 获取当前登录用户的ID，如果没有可以先写死测试
      currentUserId: this.$store.state.user.id || 1,
      receiverId: null
    };
  },
  created() {
    // 从路由参数中获取接收者（卖家）的 ID
    this.receiverId = this.$route.query.targetId;
    if(this.receiverId) {
      this.initWebSocket();
    } else {
      this.$message.error("未找到聊天对象");
    }
  },
  destroyed() {
    if (this.ws) {
      this.ws.close();
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    initWebSocket() {
      // 这里的 ip 和端口需要和你的后端地址一致
      const wsUrl = `ws://localhost:8080/websocket/chat/${this.currentUserId}`;
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log("WebSocket 连接成功");
      };

      this.ws.onmessage = (event) => {
        const receiveMsg = JSON.parse(event.data);
        // 如果消息是当前聊天对象发来的，就展示在屏幕上
        if (receiveMsg.senderId == this.receiverId) {
          this.messages.push({
            isMine: false,
            content: receiveMsg.content
          });
          this.scrollToBottom();
        }
      };

      this.ws.onerror = () => {
        console.error("WebSocket 连接发生错误");
      };
    },
    sendMessage() {
      if (!this.inputMessage.trim()) return;

      // 1. 自己发的消息展示在屏幕上
      this.messages.push({
        isMine: true,
        content: this.inputMessage
      });

      // 2. 通过 WebSocket 发给后端
      const msgObj = {
        receiverId: this.receiverId,
        content: this.inputMessage
      };
      this.ws.send(JSON.stringify(msgObj));

      this.inputMessage = '';
      this.scrollToBottom();
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
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  background-color: #f5f7f9;
}
.chat-header {
  padding: 15px;
  background-color: #fff;
  border-bottom: 1px solid #eee;
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
  border-radius: 8px 0 8px 8px;
}
.message-item.other .bubble {
  background-color: #fff;
  border-radius: 0 8px 8px 8px;
}
.bubble {
  padding: 10px 15px;
  max-width: 70%;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
.input-area {
  padding: 15px;
  background-color: #fff;
  border-top: 1px solid #eee;
}
</style>
