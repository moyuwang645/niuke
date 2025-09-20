package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String key, Page page, Model model){
        org.springframework.data.domain.Page<DiscussPost> searchResult=
            elasticsearchService.findDiscussPostById(key,page.getCurrent()-1,page.getLimit());
        List<Map<String,Object>> discussPost=new ArrayList<>();
        if(searchResult!=null){
            for(DiscussPost post:searchResult){
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                map.put("user",userService.getUserByUserid(post.getUserid()));
                map.put("likeCount",likeService.likeCount(CommentType,post.getId()));
                discussPost.add(map);
            }
        }
        model.addAttribute("discussPost",discussPost);
        model.addAttribute("key",key);
        page.setPath("/search?key="+key);
        page.setRows(searchResult==null?0:(int)searchResult.getTotalElements());
        return "/site/search";
    }
}
