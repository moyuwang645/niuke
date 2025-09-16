package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FollowerController {
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
}
