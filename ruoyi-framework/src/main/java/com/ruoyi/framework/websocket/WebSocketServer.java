package com.ruoyi.framework.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.CampusUserChat;
import com.ruoyi.system.service.ICampusUserChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/websocket/chat/{userId}")
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        sessionPool.put(userId, session);
        log.info("用户连接，用户ID：{}，当前在线人数：{}", userId, sessionPool.size());
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") Long userId) {
        log.info("收到来自用户 {} 的消息: {}", userId, message);
        try {
            JSONObject msgObj = JSON.parseObject(message);
            Long receiverId = msgObj.getLong("receiverId");
            String content = msgObj.getString("content");

            // 1. 消息入库持久化（无论接收者在线与否都保存）
            CampusUserChat chatRecord = new CampusUserChat();
            chatRecord.setSenderId(userId);
            chatRecord.setReceiverId(receiverId);
            chatRecord.setContent(content);
            chatRecord.setCreateTime(new Date());

            ICampusUserChatService chatService = SpringUtils.getBean(ICampusUserChatService.class);
            chatService.saveChat(chatRecord);

            // 2. 实时转发给接收者（若对方在线）
            Session receiverSession = sessionPool.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                JSONObject sendObj = new JSONObject();
                sendObj.put("senderId", userId);
                sendObj.put("content", content);
                sendObj.put("createTime", chatRecord.getCreateTime());
                receiverSession.getBasicRemote().sendText(sendObj.toJSONString());
            }
        } catch (Exception e) {
            log.error("解析、保存或转发消息失败", e);
        }
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        sessionPool.remove(userId);
        log.info("用户断开，用户ID：{}，当前在线人数：{}", userId, sessionPool.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 发生错误", error);
    }
}