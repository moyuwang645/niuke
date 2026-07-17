package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private  DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private HostHolder hostHolder;
    @RequestMapping(path="/index",method = RequestMethod.GET)
    public String getindex(Model model, Page page,
                           @RequestParam(value = "orderMode",defaultValue = "0") int orderMode) {
        page.setRows(discussPostService.getDisscussPostMapperCount(0));
        page.setPath("/index?orderMode="+orderMode);
        List<DiscussPost> list=discussPostService.getDisscussPostMapper(0, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts=new ArrayList<>();
        if(list!=null){
            for(DiscussPost post:list){
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                User user=userService.getUserByUserid(post.getUserid());
                map.put("user",user);
                User currentUser = hostHolder.getUser();
                long likeCount=likeService.likeCount(CommentType,post.getId());
                int likeStatus=currentUser==null?0:likeService.findLikeStatus(currentUser.getId(),CommentType,post.getId());
                map.put("likeCount",likeCount);
                map.put("likeStatus",likeStatus);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("orderMode",orderMode);
        return "index";
    }
    @RequestMapping(path = "/denied",method = RequestMethod.GET)
    public String getDenied(){
        return "site/error/404";
    }

}
