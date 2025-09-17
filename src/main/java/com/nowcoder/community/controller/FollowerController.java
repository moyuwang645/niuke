package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class FollowerController implements CommunityConstant {
    @Autowired
    private FollowService followService;
    @Autowired
    private HostHolder hostHolder;
    @RequestMapping(path = "/follow",method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType,int entityId){
        User user=hostHolder.getUser();
        followService.Follow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0,"已关注");
    }
    @RequestMapping(path = "/Unfollow",method = RequestMethod.POST)
    @ResponseBody
    public String Unfollow(int entityType,int entityId){
        User user=hostHolder.getUser();
        followService.UnFollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0,"已取消关注");
    }
    @RequestMapping(path = "/followee/{userId}",method = RequestMethod.GET)
    public String showFollowee(@PathVariable("userId")int userId, Model model, Page page){
        User user=hostHolder.getUser();
        if (user==null){
            throw new RuntimeException("未登录");
        }
        page.setLimit(5);
        page.setPath("/followee/"+user.getId());
        page.setRows((int)followService.followeeCount(userId,UserType));
        List<Map<String,Object>> followerList=followService.getFollowee(userId,page.getOffset(),page.getLimit());
        if(followerList==null){
            for(Map<String,Object> map:followerList){
                User user=(User)map.get("user");
                map.put("Followed",isFollow(user.getId()));
            }
        }
        model.addAttribute("followerList",followerList);
        return "/site/followee";
    }
    @RequestMapping(path = "/follower/{userId}",method = RequestMethod.GET)
    public String showFollower(@PathVariable("userId")int userId, Model model, Page page){
        User user=hostHolder.getUser();
        if (user==null){
            throw new RuntimeException("未登录");
        }
        page.setLimit(5);
        page.setPath("/follower/"+user.getId());
        page.setRows((int)followService.followeeCount(UserType,userId));
        List<Map<String,Object>> followerList=followService.getFollower(userId,page.getOffset(),page.getLimit());
        if(followerList==null){
            for(Map<String,Object> map:followerList){
                User user=(User)map.get("user");
                map.put("Followed",isFollow(user.getId()));
            }
        }
        model.addAttribute("followerList",followerList);
        return "/site/follower";
    }
    private boolean isFollow(int userId){
        if (hostHolder.getUser()==null){
            return false;
        }
        return followService.isFollow(hostHolder.getUser().getId(),UserType,userId);
    }
}
