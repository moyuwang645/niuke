package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
@Ignore("Requires configured SMTP credentials and sends real email.")
public class MailTests {
    @Autowired
    private MailClient mailClient;
    @Autowired
    private SpringTemplateEngine templateEngine;

    @Test
    public void sendMail() {
        mailClient.sendMail("junlinw19@gmail.com","Test","welcome");
    }

    @Test
    public void sendHtmlMail() {
        Context context = new Context();
        context.setVariable("name","wang");
        String context1 = templateEngine.process("/mail/tetmail",context);
        System.out.println(context1);
        mailClient.sendMail("junlinw19@gmail.com","Test",context1);
    }
}
