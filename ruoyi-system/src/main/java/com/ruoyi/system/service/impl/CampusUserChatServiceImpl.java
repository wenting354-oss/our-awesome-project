// 接口实现类
package com.ruoyi.system.service.impl;
import com.ruoyi.system.domain.CampusUserChat;
import com.ruoyi.system.mapper.CampusUserChatMapper;
import com.ruoyi.system.service.ICampusUserChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CampusUserChatServiceImpl implements ICampusUserChatService {
    @Autowired
    private CampusUserChatMapper chatMapper;

    @Override
    public int saveChat(CampusUserChat chat) { return chatMapper.insertChat(chat); }
    @Override
    public List<CampusUserChat> getChatHistory(Long userId, Long targetId) { return chatMapper.selectChatHistory(userId, targetId); }
    @Override
    public List<CampusUserChat> getChatSessions(Long userId) { return chatMapper.selectChatSessions(userId); }
    @Override
    public int readConversation(Long userId, Long targetId) { return chatMapper.updateMsgRead(userId, targetId); }
    @Override
    public int getTotalUnread(Long userId) { return chatMapper.selectTotalUnread(userId); }
}