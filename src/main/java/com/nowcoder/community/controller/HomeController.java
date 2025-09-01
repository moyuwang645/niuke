package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import com.nowcoder.community.entity.DiscussPost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private  DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @RequestMapping(path="/index",method = RequestMethod.GET)
    public String getindex(Model model, Page page){
        page.setRows(discussPostService.getDisscussPostMapperCount(0));
        page.setPath("/index");
        List<DiscussPost> list=discussPostService.getDisscussPostMapper(0, page.getOffset(), page.getLimit());
        List<Map<String, Object>> discussPosts=new ArrayList<>();
        if(list!=null){
            for(DiscussPost post:list){
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                User user=userService.getUserByUserid(post.getUserid());
                map.put("user",user);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        return "/index";
    }


}
