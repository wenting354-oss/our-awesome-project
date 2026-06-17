package com.ruoyi.web.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.CampusUserChat;
import com.ruoyi.system.service.ICampusUserChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campus/chat")
public class CampusChatController extends BaseController {

    @Autowired
    private ICampusUserChatService chatService;

    // 获取历史消息记录
    @GetMapping("/history/{targetId}")
    public AjaxResult getHistory(@PathVariable Long targetId) {
        Long currentUserId = SecurityUtils.getUserId();
        return AjaxResult.success(chatService.getChatHistory(currentUserId, targetId));
    }

    // 获取当前登录用户的所有聊天会话列表
    @GetMapping("/sessions")
    public AjaxResult getSessions() {
        Long currentUserId = SecurityUtils.getUserId();
        return AjaxResult.success(chatService.getChatSessions(currentUserId));
    }

    // 将某会话标记为已读
    @PutMapping("/read/{targetId}")
    public AjaxResult readConversation(@PathVariable Long targetId) {
        Long currentUserId = SecurityUtils.getUserId();
        chatService.readConversation(currentUserId, targetId);
        return AjaxResult.success();
    }

    // 获取总未读数
    @GetMapping("/unread/total")
    public AjaxResult getTotalUnread() {
        Long currentUserId = SecurityUtils.getUserId();
        return AjaxResult.success(chatService.getTotalUnread(currentUserId));
    }
}