package com.nowcoder.community.controller;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;
    @RequestMapping(path="/add/{discussPostId}",method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);
        Event event=new Event()
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setMap("discussPostId",discussPostId);
        if(comment.getEntityType()==CommentType){
            DiscussPost discussPost=discussPostService.getDiscussPostById(comment.getEntityId());
            event.setEntityUserId(discussPost.getUserid());
        }else if(comment.getEntityType()==ReplyType){
            Comment recomment=commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(recomment.getUserId());
        }
        eventProducer.fireEvent(event);
        if(comment.getEntityType()==CommentType){
            Event addEvent = new Event()
                    .setTopic(Publish)
                    .setUserId(comment.getUserId())
                    .setEntityType(CommentType)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(addEvent);
        }
        String redisKey= RedisUtil.getPostScore();
        redisTemplate.opsForSet().add(redisKey, discussPostId);
        return "redirect:/discuss/detail/"+discussPostId;

    }
}
