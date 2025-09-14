package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface MessageMapper {
    List<Message> selectConversations(int userId,int offset,int limit);
    int selectCountConversation(int userId);
    List<Message> selectLetters(String conversationId,int offset,int limit);
    int selectCountLetters(String conversationId);
    int selectLetterUnreadCount(int userId,String conversationId);
    int insertMessage(Message message);
    int updateStatus(List<Integer> ids,int status);
}
