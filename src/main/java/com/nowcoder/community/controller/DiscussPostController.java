package com.nowcoder.community.controller;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
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

import java.util.Date;

@Controller
@RequestMapping(path = "discuss")
public class DiscussPostController {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;

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
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId,Model model){
        DiscussPost discussPost= discussPostService.getDiscussPostById(discussPostId);
        model.addAttribute("discussPost",discussPost);
        User user= userService.getUserByUserid(discussPost.getUserid());
        model.addAttribute("user",user);
        return "/site/discuss-detail";

    }
}
