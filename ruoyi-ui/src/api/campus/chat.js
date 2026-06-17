import request from '@/utils/request'

// 获取与特定用户的聊天历史记录
export function getChatHistory(targetId) {
  return request({
    url: '/campus/chat/history/' + targetId,
    method: 'get'
  })
}

// 获取用户的简要会话列表
export function getChatSessions() {
  return request({
    url: '/campus/chat/sessions',
    method: 'get'
  })
}

// 标记单人消息为已读
export function markAsRead(targetId) {
  return request({
    url: '/campus/chat/read/' + targetId,
    method: 'put'
  })
}

// 获取总的未读消息数
export function getTotalUnread() {
  return request({
    url: '/campus/chat/unread/total',
    method: 'get'
  })
}
