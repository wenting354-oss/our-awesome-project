// 接口定义
package com.ruoyi.system.service;
import com.ruoyi.system.domain.CampusUserChat;
import java.util.List;

public interface ICampusUserChatService {
    int saveChat(CampusUserChat chat);
    List<CampusUserChat> getChatHistory(Long userId, Long targetId);
    List<CampusUserChat> getChatSessions(Long userId);
    int readConversation(Long userId, Long targetId);
    int getTotalUnread(Long userId);
}