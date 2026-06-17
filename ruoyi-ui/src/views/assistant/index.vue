<template>
  <div class="assistant-container">
    <el-card class="chat-card" shadow="never">
      <div slot="header" class="chat-header">
        <i class="el-icon-cpu" style="font-size: 20px; margin-right: 8px; color: #409EFF;"></i>
        <span>校园全能 AI 助手</span>
        <el-tag size="mini" type="primary" style="margin-left: 10px;">Agent 工具模式</el-tag>
      </div>

      <div class="chat-body" ref="chatBody">
        <div v-for="(item, index) in chatHistory" :key="index" :class="['message-item', item.from === 'user' ? 'is-user' : 'is-ai']">
          <el-avatar :size="40" :src="item.from === 'user' ? userAvatar : aiAvatar" class="avatar"></el-avatar>

          <div class="message-bubble">
            <div class="message-content">{{ item.content }}</div>
          </div>
        </div>

        <div v-if="loading" class="message-item is-ai">
          <el-avatar :size="40" :src="aiAvatar" class="avatar"></el-avatar>
          <div class="message-bubble typing-bubble">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            <span class="typing-text">{{ loadingText }}</span>
          </div>
        </div>
      </div>

      <div class="chat-footer">
        <el-input
          v-model="message"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="试试问我任何问题，比如：帮我发个取快递任务，给5块钱..."
          @keydown.native.enter.exact.prevent="handleSend"
          :disabled="loading"
        ></el-input>
        <div class="action-bar">
          <el-button icon="el-icon-delete" size="small" @click="clearHistory" :disabled="loading" plain>
            清空对话
          </el-button>
          <el-button type="primary" icon="el-icon-s-promotion" size="small" @click="handleSend" :loading="loading">
            发 送
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
// 确保这些 API 在 PC 端的路径是一致的
import { listSchedule } from '@/api/schedule'
import { callQwenAI, agentTools } from '@/utils/qwen'

export default {
  name: "AssistantChat",
  data() {
    return {
      message: '',
      loading: false,
      loadingText: 'AI 思考中...',
      // UI 展示用的历史记录
      chatHistory: [
        {
          from: 'ai',
          content: '您好！我是校园全能智能助手 🤖\n\n我可以帮您：\n📅 查询课表（如"今天有什么课"）\n🏃 发布跑腿任务（如"帮我发个取快递任务，给5元"）\n💬 发布圈子帖子（如"帮我发个失物招领帖"）\n🛍️ 发布二手商品（如"帮我卖二手高数书"）\n🔍 搜索二手商品（如"帮我找一下有没有卖自行车的"）\n\n您想让我帮您做什么？'
        }
      ],
      // 发给 AI 的真实上下文历史
      conversationHistory: [],
      aiAvatar: require('@/assets/logo/logo.png'), // AI 头像
    }
  },
  computed: {
    userAvatar() {
      const avatar = this.$store.state.user.avatar;
      return avatar ? process.env.VUE_APP_BASE_API + avatar : require('@/assets/images/profile.jpg');
    }
  },
  methods: {
    async handleSend() {
      const msg = this.message.trim()
      if (!msg) return

      // 1. 追加用户消息
      this.chatHistory.push({ from: 'user', content: msg })
      this.conversationHistory.push({ role: 'user', content: msg })

      this.message = ''
      this.loading = true
      this.loadingText = 'AI 思考中...'
      this.scrollToBottom()

      try {
        // 2. 核心：调用 Agent 处理
        const aiResponse = await this.callQwenAgent(msg)
        this.chatHistory.push({ from: 'ai', content: aiResponse })
      } catch (e) {
        console.error("AI 交互异常:", e);
        this.chatHistory.push({ from: 'ai', content: '抱歉，我遇到了一些网络或接口问题，请稍后再试。' })
      }

      this.loading = false
      this.scrollToBottom()
    },

    async callQwenAgent(userMessage) {
      // 第一轮调用：携带 tools 清单问 AI
      const response = await callQwenAI(this.conversationHistory, agentTools)

      if (!response.success) {
        return '抱歉，AI服务暂时不可用：' + response.error
      }

      const choice = response.data.choices[0]
      const assistantMessage = choice.message

      // 检查 AI 是否决定调用工具 (Function Calling)
      if (assistantMessage.tool_calls && assistantMessage.tool_calls.length > 0) {

        const toolCall = assistantMessage.tool_calls[0]
        const functionName = toolCall.function.name
        const functionArgs = JSON.parse(toolCall.function.arguments)

        console.log('💡 AI 决定调用工具:', functionName, functionArgs)
        this.loadingText = '正在执行后台操作，请稍候...'

        // 执行本地工具方法，将结果存放到 toolResult
        const toolResult = await this.executeToolCall(functionName, functionArgs)

        // 将工具的调用过程和返回结果都塞入上下文，发给 AI 进行最后总结
        this.conversationHistory.push({
          role: 'assistant',
          content: '',
          tool_calls: assistantMessage.tool_calls
        })
        this.conversationHistory.push({
          role: 'tool',
          content: toolResult,
          tool_call_id: toolCall.id
        })

        this.loadingText = '正在整理操作结果...'

        // 第二轮调用：让 AI 根据工具执行的结果，组织人类语言回复
        const finalResponse = await callQwenAI(this.conversationHistory)
        if (finalResponse.success) {
          const finalMessage = finalResponse.data.choices[0].message.content
          this.conversationHistory.push({ role: 'assistant', content: finalMessage })
          return finalMessage
        } else {
          return toolResult // 如果第二轮总结失败，直接把工具执行的原生结果扔给用户
        }

      } else {
        // AI 认为不需要工具，直接进行了纯文本回复
        const content = assistantMessage.content
        this.conversationHistory.push({ role: 'assistant', content: content })
        return content
      }
    },

    // 执行具体的工具映射
    async executeToolCall(functionName, args) {
      if (functionName === 'query_schedule') {
        return await this.getSchedule(args.day_type)
      } else if (functionName === 'create_errand_task') {
        return await this.createErrandTask(args)
      } else if (functionName === 'create_circle_post') {
        return await this.createCirclePost(args)
      } else if (functionName === 'create_secondhand_product') {
        return await this.createSecondhandProduct(args)
      } else if (functionName === 'search_secondhand_product') {
        return await this.searchSecondhandProduct(args)
      } else if (functionName === 'buy_secondhand_product') {
        return await this.buySecondhandProduct(args)
      }
      return '未知工具调用'
    },

    // ================= 以下为具体的业务工具方法 =================

    async getSchedule(dayType) {
      try {
        const res = await listSchedule()
        if (res.code !== 200 || !res.data) return '📅 获取课表失败'

        const dayNum = this.getDayNum(dayType)
        const courses = res.data.filter(c => c.day === dayNum).sort((a, b) => a.section - b.section)
        const dayName = this.getDayName(dayType)

        if (courses.length === 0) return `📅 ${dayName}没有课程安排`

        let result = `📅 ${dayName}的课程：\n\n`
        courses.forEach(c => {
          result += `🕐 第${c.section}节: ${c.name}`
          if (c.teacher) result += ` | ${c.teacher}`
          if (c.location) result += ` | ${c.location}`
          result += '\n'
        })
        return result
      } catch (e) {
        return '📅 获取课表接口异常'
      }
    },

    async createErrandTask(args) {
      try {
        const { addErrand } = await import('@/api/campus/errand')
        const data = {
          orderType: args.task_type,
          title: args.title,
          detail: args.detail,
          reward: args.reward,
          deliveryAddress: args.delivery_address,
          status: '0'
        }
        const res = await addErrand(data)
        if (res.code === 200) {
          return `✅ 任务发布成功！\n\n📋 标题：${args.title}\n💰 悬赏：¥${args.reward}\n📍 地址：${args.delivery_address}`
        } else {
          return `❌ 发布失败：${res.msg}`
        }
      } catch (e) {
        return `❌ 任务发布出现异常`
      }
    },

    async createCirclePost(args) {
      try {
        const { addTopic } = await import('@/api/campus/topic')
        const data = {
          topicType: args.category,
          content: `${args.title}\n\n${args.content}`,
          status: '0',
          commentEnabled: '0'
        }
        const res = await addTopic(data)
        if (res.code === 200) {
          return `✅ 帖子发布成功！\n\n📝 标题：${args.title}\n📚 分类：${args.category}`
        } else {
          return `❌ 发布失败：${res.msg}`
        }
      } catch (e) {
        return `❌ 帖子发布出现异常`
      }
    },

    async createSecondhandProduct(args) {
      try {
        // 注意：PC 端 API 如果是 /api/campus/product，需要确保这里引入的路径对得上
        const { addProduct } = await import('@/api/campus/product')
        const data = {
          title: args.title,
          description: args.description,
          category: args.category,
          price: args.price,
          contactInfo: args.contact_info,
          status: '0'
        }
        const res = await addProduct(data)
        if (res.code === 200) {
          return `✅ 二手商品上架成功！\n\n📦 名称：${args.title}\n💰 价格：￥${args.price}\n\n💡 请去“我的发布”补充真实图片。`
        } else {
          return `❌ 发布失败：${res.msg}`
        }
      } catch (e) {
        return `❌ 商品发布出现异常`
      }
    },

    async searchSecondhandProduct(args) {
      try {
        const { listProducts } = await import('@/api/campus/product')
        const query = { pageNum: 1, pageSize: 10 }

        if (args.category && args.category !== '全部') query.category = args.category
        if (args.keyword) query.title = args.keyword

        const res = await listProducts(query)
        if (res.code === 200 && res.rows) {
          if (res.rows.length === 0) return `🔍 市场里暂时没搜到相关商品哦。`

          let result = `🔍 为您找到 ${res.total} 件相关商品：\n\n`
          res.rows.slice(0, 10).forEach((item, index) => {
            result += `${index + 1}. 【${item.title}】 ￥${item.price}\n`
          })
          result += `\n如需购买，请前往二手商城大厅查看详情。`
          return result
        } else {
          return `❌ 搜索商品失败：${res.msg}`
        }
      } catch (e) {
        return `❌ 搜索服务异常`
      }
    },

    async buySecondhandProduct(args) {
      try {
        const { createOrder } = await import('@/api/campus/order')
        const data = {
          productId: args.product_id,
          address: args.address,
          remark: args.remark || ''
        }
        const res = await createOrder(data)
        if (res.code === 200) {
          return `✅ 下单成功！\n\n📍 送货地址：${args.address}\n系统已通知卖家发货。`
        } else {
          return `❌ 下单失败：${res.msg}`
        }
      } catch (e) {
        return `❌ 下单服务异常`
      }
    },

    // ================= 辅助方法 =================

    getDayNum(t) {
      const today = new Date().getDay() || 7
      const map = { 'today': today, 'tomorrow': (today % 7) + 1, 'monday': 1, 'tuesday': 2, 'wednesday': 3, 'thursday': 4, 'friday': 5, 'saturday': 6, 'sunday': 7 }
      return map[t] || today
    },
    getDayName(t) {
      const map = { 'today': '今天', 'tomorrow': '明天', 'monday': '星期一', 'tuesday': '星期二', 'wednesday': '星期三', 'thursday': '星期四', 'friday': '星期五', 'saturday': '星期六', 'sunday': '星期日' }
      return map[t] || '今天'
    },

    // 清空历史
    clearHistory() {
      this.$confirm('确定要清空上下文记忆吗？', '提示', { type: 'warning' }).then(() => {
        // 保留第一句打招呼
        this.chatHistory = [this.chatHistory[0]];
        this.conversationHistory = [];
        this.$message.success('上下文已清除');
      }).catch(() => {});
    },

    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const chatBody = this.$refs.chatBody;
        if (chatBody) {
          chatBody.scrollTop = chatBody.scrollHeight;
        }
      });
    }
  }
}
</script>

<style scoped>
.assistant-container {
  padding: 0; /* 去掉边距，紧贴抽屉边缘 */
  height: 100vh; /* 撑满抽屉的高度 */
  background-color: #f4f6f8;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
}

::v-deep .el-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.chat-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f7f8fa;
  scroll-behavior: smooth;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

.message-item.is-user {
  flex-direction: row-reverse;
}

.message-item.is-user .message-bubble {
  background-color: #95ec69;
  color: #000;
  border-radius: 10px 0 10px 10px;
  margin-right: 15px;
}

.message-item.is-ai .message-bubble {
  background-color: #fff;
  color: #333;
  border-radius: 0 10px 10px 10px;
  margin-left: 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  position: relative;
  line-height: 1.6;
}

/* 保证 Markdown / \n 换行符可以正常显示 */
.message-content {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
}

/* 加载动画 */
.typing-bubble {
  display: flex;
  align-items: center;
  color: #909399 !important;
  font-size: 13px !important;
}
.typing-text { margin-left: 8px; }
.dot {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: #909399;
  margin: 0 2px;
  animation: typing 1.4s infinite ease-in-out both;
}
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-footer {
  padding: 15px 20px;
  background-color: #fff;
  border-top: 1px solid #ebeef5;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
</style>
