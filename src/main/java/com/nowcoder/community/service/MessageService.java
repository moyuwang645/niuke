package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> selectConversations(int userId,int offset,int limit){
        return messageMapper.selectConversations(userId,offset,limit);
    }
    public int selectCountConversation(int userId){
        return messageMapper.selectCountConversation(userId);
    }
    public List<Message> selectLetters(String conversationId,int offset,int limit){
        return messageMapper.selectLetters(conversationId,offset,limit);
    }
    public int selectCountLetters(String conversationId){
        return messageMapper.selectCountLetters(conversationId);
    }
    public int selectUnreadCount(int userId,String conversationId){
        return messageMapper.selectLetterUnreadCount(userId,conversationId);
    }
    public int addMessage(Message message){
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }
    public int readMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids,1);
    }
    public Message selectLastNotice(int userId,String topic){
        return messageMapper.selectLastNotice(userId,topic);
    }
    public int selectNoticeUnreadCount(int userId,String topic){
        return messageMapper.selectNoticeUnreadCount(userId,topic);
    }
    public int selectNoticeCount(int userId,String topic){
        return messageMapper.selectNoticeCount(userId,topic);
    }
    public List<Message> selectNotice(int userId,String topic,int offset,int limit){
        return messageMapper.selectNotice(userId,topic,offset,limit);
    }
}
