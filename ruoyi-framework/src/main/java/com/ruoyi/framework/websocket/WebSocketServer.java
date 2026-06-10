package com.ruoyi.framework.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 实时通讯服务
 */
@Component
@ServerEndpoint("/websocket/chat/{userId}")
public class WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    // 保存所有在线用户的 Session (Key: 用户ID, Value: Session)
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

            // TODO: 在这里你可以通过 SpringUtils.getBean() 获取 Service，将消息保存到 campus_user_chat 数据库表中

            // 实时转发给接收者（如果对方在线）
            Session receiverSession = sessionPool.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                JSONObject sendObj = new JSONObject();
                sendObj.put("senderId", userId);
                sendObj.put("content", content);
                receiverSession.getBasicRemote().sendText(sendObj.toJSONString());
            }
        } catch (Exception e) {
            log.error("解析或转发消息失败", e);
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