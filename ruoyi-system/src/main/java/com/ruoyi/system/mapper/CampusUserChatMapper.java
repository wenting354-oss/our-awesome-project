package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.CampusUserChat;
import org.apache.ibatis.annotations.Param; // 👈 已经修正了这里的导包错误
import java.util.List;

public interface CampusUserChatMapper {
    // 插入聊天记录
    int insertChat(CampusUserChat chat);

    // 查询两个用户间的历史聊天记录
    List<CampusUserChat> selectChatHistory(@Param("userId") Long userId, @Param("targetId") Long targetId);

    // 查询当前用户的聊天会话列表（包括联系人昵称、头像、最后一条消息、未读数）
    List<CampusUserChat> selectChatSessions(@Param("userId") Long userId);

    // 将某联系人发给当前用户的消息全部标为已读
    int updateMsgRead(@Param("userId") Long userId, @Param("targetId") Long targetId);

    // 获取当前用户的未读总消息数
    int selectTotalUnread(Long userId);
}