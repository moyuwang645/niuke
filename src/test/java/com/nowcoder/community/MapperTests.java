package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.LoginTicketMapping;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper disscussPostMapper;

    @Autowired
    private LoginTicketMapping loginTicketMapping;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(101);
        System.out.println(user);

        user = userMapper.selectByName("liubei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());
    }

    @Test
    public void updateUser() {
        int rows = userMapper.updateStatus(150, 1);
        System.out.println(rows);

        rows = userMapper.updateHeader(150, "http://www.nowcoder.com/102.png");
        System.out.println(rows);

        rows = userMapper.updatePassword(150, "hello");
        System.out.println(rows);
    }

    @Test
    public void testSelectDiscussPost() {
        List<DiscussPost> list=disscussPostMapper.SelectDiscussPost(149,0,10);
        for(DiscussPost post:list){
            System.out.println(post);
        }
        int rows= disscussPostMapper.SelectDiscussPostRows(149);
        System.out.println(rows);
    }

    @Test
    public void testInsertDiscussPost() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setTicket("123456");
        loginTicket.setExpire(new Date(System.currentTimeMillis()+1600));
        loginTicket.setStatus(0);
        loginTicket.setUserId(101);
        loginTicketMapping.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket() {
        LoginTicket loginTicket =new LoginTicket();
        loginTicket = loginTicketMapping.selectLoginTicket("123456");
        System.out.println(loginTicket);
        loginTicketMapping.updateLoginTicket(loginTicket,1);
        loginTicket = loginTicketMapping.selectLoginTicket("123456");
        System.out.println(loginTicket);
    }
}


