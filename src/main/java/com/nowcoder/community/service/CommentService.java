package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.SensitiveFilter;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpUtils;
import java.util.List;

import static com.nowcoder.community.util.CommunityConstant.CommentType;

@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    public List<Comment> findCommentService(int entityType, int entityId,int offset,int limit){
        return commentMapper.selectCommentByEntity(entityType,entityId,offset,limit);
    }
    public int countCommentService(int entityType, int entityId){
        return commentMapper.selectCountEntity(entityType,entityId);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if(comment == null){
            throw new IllegalArgumentException("无评论");
        }
        comment.setContent(HtmlUtils.htmlUnescape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int row=commentMapper.insertComment(comment);
        if(comment.getEntityType()==CommentType){
            int count=commentMapper.selectCountEntity(comment.getEntityType(),comment.getEntityId());
            discussPostMapper.updateCommentCount(comment.getEntityId(),count);
        }
        return row;
    }
    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }
}
