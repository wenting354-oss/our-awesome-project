import { getToken } from '@/utils/auth'

// ⚠️ 这里的地址 '/dify-api' 需要在 vue.config.js 中配置代理转发，见第三步
// ⚠️ 请将下面的 'Bearer app-xxxx' 替换为您在 Dify 平台创建应用后获取的 [API 密钥]
const DIFY_API_KEY = 'Bearer app-rzanYSCizwdhJHuASP7nHwib';

/**
 * 发送消息到 Dify (流式响应)
 * @param {string} query 用户输入的内容
 * @param {string} conversationId 会话ID (用于保持上下文)
 */
export function sendToDify(query, conversationId = null) {
  // 获取若依当前登录用户的 Token (用于传递给后端接口鉴权)
  const userToken = 'Bearer ' + getToken();

  // 使用原生 fetch 以支持流式读取
  return fetch('/dify-api/chat-messages', {
    method: 'POST',
    headers: {
      'Authorization': DIFY_API_KEY,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      inputs: {
        // 【关键】将若依的用户 Token 作为变量传给 Dify Agent
        "sys_token": userToken,
        "user_question": query
      },
      query: query,
      response_mode: "streaming", // 开启流式模式
      conversation_id: conversationId,
      user: "ruoyi-user" // 标识用户身份
    })
  });
}

/**
 * 获取聊天历史记录 (保留原有的或改为从 Dify 获取)
 */
export function getChatHistory() {
  // 如果需要从 Dify 获取历史，可以使用 Dify 的 /messages 接口
  // 这里暂时保留您原有的空实现或后端实现
  return Promise.resolve({ data: [] });
}
