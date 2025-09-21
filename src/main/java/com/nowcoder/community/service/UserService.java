package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapping;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.xml.crypto.Data;
import java.util.*;

@Service
public class UserService  implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private LoginTicketMapping loginTicketMapping;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    public User getUserByUserid(int userid) {
        return userMapper.selectById(userid);
    }
    public Map<String,Object> register (User user) {
        Map<String,Object> map = new HashMap<>();
        if (user == null) {
            throw new IllegalArgumentException("用户为空");
        }
        if(StringUtils.isEmpty(user.getUsername())) {
            map.put("usermsg","账号为空");
            return map;
        }
        if(StringUtils.isEmpty(user.getPassword())) {
            map.put("passwordmsg","密码为空");
            return map;
        }
        if(StringUtils.isEmpty(user.getEmail())) {
            map.put("mailmsg","邮箱为空");
            return map;
        }

        User u=userMapper.selectByName(user.getUsername());
        if(u!=null) {
            map.put("usermsg","账号已存在");
            return map;
        }
        u=userMapper.selectByEmail(user.getEmail());
        if(u!=null) {
            map.put("usermail","邮箱已注册");
            return map;
        }

        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setStatus(0);
        user.setType(0);
        userMapper.insertUser(user);

        Context context=new Context();
        context.setVariable("email",user.getEmail());
        String url=domain+"/"+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        String html=templateEngine.process("mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号",html);
        return map;
    }
    public int activation(int userid,String code){
        User user=userMapper.selectById(userid);
        if(user.getStatus()==1){
            return activation_repeat;
        } else if (user.getActivationCode().equals(code)) {
            user.setStatus(1);
            return activation_success;
        }else{
            return activation_fail;
        }
    }
    public Map<String,Object> LoginService(String userid, String password, int expireddata){
        Map<String,Object> map = new HashMap<>();
        if(StringUtils.isEmpty(userid)){
            map.put("usermsg","账号不能为空");
            return map;
        }
        if(StringUtils.isEmpty(password)){
            map.put("passwordmsg","，“密码不能为空");
            return map;
        }
        User user=userMapper.selectByName(userid);
        if(user==null){
            map.put("usermsg","账号不存在");
            return map;
        }
        if(user.getStatus()==0){
            map.put("usermsg","账号未激活");
            return map;
        }
        if(!user.getPassword().equals(CommunityUtil.md5(password+user.getSalt()))) {
            map.put("passwordmsg", "密码错误");
            return map;
        }
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setUserId(user.getId());
        loginTicket.setExpire(new Date(System.currentTimeMillis()+expireddata));
        loginTicket.setStatus(0);
        loginTicketMapping.insertLoginTicket(loginTicket);
        map.put("ticket",loginTicket.getTicket());
        return map;
    }
    public void logout(String ticket){
        loginTicketMapping.updateLoginTicket(ticket,1);

    }
    public LoginTicket selectloginticket(String ticket){
        return loginTicketMapping.selectLoginTicket(ticket);
    }

    public int updateHesder(int userid,String hesderurl){
        return userMapper.updateHeader(userid,hesderurl);
    }
    public User getUserByName(String username) {
        return userMapper.selectByName(username);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userid) {
        User user=userMapper.selectById(userid);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AuthorityAdmin;
                    case 2:
                        return AuthorityModerator;
                    default:
                        return AuthorityUser;
                }
            }
        });
        return authorities;
    }
}
