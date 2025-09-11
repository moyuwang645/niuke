package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Map;

@Controller
public class LoginContreoller implements CommunityConstant {
    @Autowired
    private UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    private static final Logger logger = LoggerFactory.getLogger(LoginContreoller.class);

    public LoginContreoller(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String getResgterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String getloginPage(){
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
    @RequestMapping(path="/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        response.setDateHeader("Expires", 0);
        String text=kaptchaProducer.createText();
        BufferedImage image=kaptchaProducer.createImage(text);
        session.setAttribute("kaptcha", text);
        response.setContentType("image/png");
        try {
            OutputStream os=response.getOutputStream();
            ImageIO.write(image,"png",os);
        }catch (Exception e){
            logger.error("输出失败"+e.getMessage());
        }
    }

    @RequestMapping(path="/login",method = RequestMethod.POST)
    public String login(Model model, String username, String password, boolean remember, HttpSession session
                        , HttpServletResponse response,String code){
        String kaptcha=(String) session.getAttribute("kaptcha");
        if(StringUtils.isEmpty(kaptcha)||!kaptcha.equalsIgnoreCase(code)||StringUtils.isEmpty(code)){
            model.addAttribute("codemsg","验证码不正确");
            return "/site/login";
        }
        int expiredtime = remember?MAX_REMEMBER_TIME_REMEBER:MAX_REMEMBER_TIME;
        Map<String,Object> map = userService.LoginService(username,password,expiredtime);
        if(map.containsKey("ticket")){
            Cookie ck=new Cookie("ticket",map.get("ticket").toString());
            ck.setPath(contextPath);
            ck.setMaxAge(expiredtime);
            response.addCookie(ck);
            return "redirect:/index";
        }else{
            model.addAttribute("usermsg",map.get("usermsg"));
            model.addAttribute("passwordmsg",map.get("passwordmsg"));
            return  "/site/login";
        }
    }

    @RequestMapping(path="/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }
}
