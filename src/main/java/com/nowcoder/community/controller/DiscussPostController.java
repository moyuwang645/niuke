package com.nowcoder.community.controller;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

import static com.nowcoder.community.util.CommunityConstant.CommentType;
import static com.nowcoder.community.util.CommunityConstant.ReplyType;

@Controller
@RequestMapping(path = "discuss")
public class DiscussPostController {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addPost(String content,String title){
        User user = hostHolder.getUser();
        if(user==null){
            return CommunityUtil.getJSONString(403,"未登录");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserid(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);
        return CommunityUtil.getJSONString(0,"发布成功");
    }
    @RequestMapping(value = "/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        DiscussPost discussPost= discussPostService.getDiscussPostById(discussPostId);
        model.addAttribute("discussPost",discussPost);
        User user= userService.getUserByUserid(discussPost.getUserid());
        model.addAttribute("user",user);
        long likeCount=likeService.likeCount(CommentType,discussPost.getId());
        long likeStatus=hostHolder.getUser()==null?0:
                likeService.findLikeStatus(hostHolder.getUser().getId(),CommentType,discussPost.getId());
        model.addAttribute("likeCount",likeCount);
        model.addAttribute("likeStatus",likeStatus);
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(discussPost.getCommentCount());
        List<Comment> commentList=commentService.findCommentService(
                CommentType,discussPost.getId(), page.getOffset(), page.getLimit()
        );
        List<Map<String,Object>> commentVOList=new ArrayList<>();
        if(commentList!=null){
            for(Comment comment:commentList){
                Map<String,Object> commentVO=new HashMap<>();
                commentVO.put("comment",comment);
                commentVO.put("user",userService.getUserByUserid(comment.getUserId()));
                likeCount=likeService.likeCount(CommentType,comment.getId());
                likeStatus=hostHolder.getUser()==null?0:
                        likeService.findLikeStatus(hostHolder.getUser().getId(),CommentType,comment.getId());
                commentVO.put("likeCount",likeCount);
                commentVO.put("likeStatus",likeStatus);
                List<Comment> replyList=commentService.findCommentService(
                        ReplyType,comment.getId(),0,Integer.MAX_VALUE
                );
                List<Map<String,Object>> replyVOList=new ArrayList<>();
                if(replyList!=null)
                    for(Comment reply:replyList){
                        Map<String,Object> replyVO=new HashMap<>();
                        replyVO.put("reply",reply);
                        replyVO.put("user",userService.getUserByUserid(reply.getUserId()));
                        User target=reply.getTargetId()==0?null:userService.getUserByUserid(reply.getTargetId());
                        replyVO.put("target",target);
                        likeCount=likeService.likeCount(CommentType,reply.getId());
                        likeStatus=hostHolder.getUser()==null?0:
                                likeService.findLikeStatus(hostHolder.getUser().getId(),CommentType,reply.getId());
                        replyVO.put("likeCount",likeCount);
                        replyVO.put("likeStatus",likeStatus);
                        replyVOList.add(replyVO);
                    }
                commentVO.put("replys",replyVOList);
                int replyCount=commentService.countCommentService(ReplyType,comment.getUserId());
                commentVO.put("replyCount",replyCount);
                commentVOList.add(commentVO);
            }
        }
        model.addAttribute("comments",commentVOList);
        return "site/discuss-detail";
    }
}
