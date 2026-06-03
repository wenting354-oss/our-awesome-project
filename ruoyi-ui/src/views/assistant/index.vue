<template>
  <div class="chat-container">
    <div class="message-list" ref="messageList">
      <div v-for="(message, index) in messages" :key="index" :class="['message-item', message.role]">
        <div class="content-wrapper">
          <div class="bubble">
            <p v-html="renderMessage(message.content)"></p>
          </div>
          <span class="time" v-if="message.createTime">{{ message.createTime }}</span>
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input
        v-model="inputMessage"
        @keyup.enter.native="sendMessage"
        placeholder="请输入消息，例如：帮我查下课表..."
        :disabled="loading"
      >
        <el-button slot="append" icon="el-icon-position" @click="sendMessage" :loading="loading">发送</el-button>
      </el-input>
    </div>
  </div>
</template>

<script>
import { sendToDify, getChatHistory } from "@/api/assistant/chat";
import { parseTime } from "@/utils/ruoyi";

export default {
  name: "Chat",
  data() {
    return {
      inputMessage: '',
      messages: [],
      loading: false,
      conversationId: null, // 新增：保存会话ID以维持上下文
    };
  },
  created() {
    this.messages.push({
      role: 'assistant',
      content: '你好！我是智慧校园助手，我可以帮你查课表、发帖子、发跑腿订单。请问有什么可以帮您？',
      createTime: parseTime(new Date())
    });
  },
  methods: {
    // 简单的换行处理，进阶可引入 markdown-it
    renderMessage(content) {
      if (!content) return '';
      return content.replace(/\n/g, '<br>');
    },

    // 发送消息
    async sendMessage() {
      const content = this.inputMessage.trim();
      if (!content) return;

      // 1. 用户消息上屏
      this.messages.push({
        role: 'user',
        content: content,
        createTime: parseTime(new Date())
      });
      this.inputMessage = '';
      this.scrollToBottom();
      this.loading = true;

      try {
        // 2. 添加一个空的 AI 消息占位，准备接收流式输出
        const aiMessageIndex = this.messages.push({
          role: 'assistant',
          content: '...', // 正在输入状态
          createTime: parseTime(new Date())
        }) - 1;

        // 3. 调用 API
        const response = await sendToDify(content, this.conversationId);

        // 检查响应状态
        if (!response.ok) {
          throw new Error("API 请求失败");
        }

        // 4. 处理流式响应
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let isFirstChunk = true;

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value);
          const lines = chunk.split('\n');

          lines.forEach(line => {
            if (line.startsWith('data: ')) {
              try {
                const jsonStr = line.slice(6);
                if (jsonStr === '[DONE]') return;

                const data = JSON.parse(jsonStr);

                // 保存 conversation_id
                if (data.conversation_id) {
                  this.conversationId = data.conversation_id;
                }

                // 拼接回答内容 (event: message 或 agent_message)
                if (data.event === 'message' || data.event === 'agent_message') {
                  // 如果是第一帧数据，清空占位符
                  if (isFirstChunk) {
                    this.messages[aiMessageIndex].content = "";
                    isFirstChunk = false;
                  }
                  this.messages[aiMessageIndex].content += data.answer;
                  // 实时滚动到底部
                  this.scrollToBottom();
                }
              } catch (e) {
                console.warn("解析流数据出错", e);
              }
            }
          });
        }
      } catch (error) {
        console.error(error);
        // 如果出错，更新最后一条消息为错误提示
        const lastMsg = this.messages[this.messages.length - 1];
        if (lastMsg.role === 'assistant') {
          lastMsg.content += "\n[连接出现问题，请稍后再试]";
        }
      } finally {
        this.loading = false;
        this.scrollToBottom();
      }
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

<style scoped lang="scss">
/* 保持原有样式，仅微调 input-area */
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  border: 1px solid #e6ebf5;
  border-radius: 4px;
  background-color: #fff;
  margin: 10px;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;

  .message-item {
    display: flex;
    margin-bottom: 20px;

    .content-wrapper {
      display: flex;
      flex-direction: column;
      max-width: 80%; /* 稍微加宽一点 */
    }

    .bubble {
      padding: 10px 15px;
      border-radius: 8px;
      line-height: 1.6;
      word-wrap: break-word;
      font-size: 14px;
      box-shadow: 0 1px 2px rgba(0,0,0,0.1);

      /* 让 v-html 里的 p 标签不带默认 margin */
      ::v-deep p {
        margin: 0;
      }
    }

    .time {
      font-size: 12px;
      color: #999;
      margin-top: 5px;
    }

    &.user {
      justify-content: flex-end;
      .content-wrapper { align-items: flex-end; }
      .bubble {
        background-color: #95ec69;
        color: #000;
        border-top-right-radius: 2px;
      }
    }

    &.assistant {
      justify-content: flex-start;
      .content-wrapper { align-items: flex-start; }
      .bubble {
        background-color: #ffffff;
        color: #333;
        border-top-left-radius: 2px;
      }
    }
  }
}

.input-area {
  padding: 15px;
  border-top: 1px solid #eee;
  background-color: #fff;
}
</style>
