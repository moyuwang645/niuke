package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.MappingMatch;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginContreoller implements CommunityConstant {
    private final UserService userService;

    public LoginContreoller(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String geyResgterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String geyloginPage(){
        return "/site/login";
    }
    @RequestMapping(path="/register",method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String,Object> map=userService.register(user);
        if(map.isEmpty()){
            model.addAttribute("msg","注册成功");
            model.addAttribute("target","/index");
            return "site/operate-result";
        }else{
            model.addAttribute("usermsg",map.get("usermsg"));
            model.addAttribute("passwordmsg",map.get("passwordmsg"));
            model.addAttribute("mailmsg",map.get("mailmsg"));
            return "site/register";
        }
    }
    @RequestMapping(path="/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(@PathVariable("userId") int userId, @PathVariable("code") String code, Model model){
        int result=userService.activation(userId,code);
        if(result==activation_success){
            model.addAttribute("msg","激活成功");
            model.addAttribute("target","/index");
        } else if (result==activation_repeat) {
            model.addAttribute("msg","重复激活");
            model.addAttribute("target","/index");
        }else{
            model.addAttribute("msg","激活失败");
            model.addAttribute("target","/index");
        }
        return "site/operate-result";
    }
}
