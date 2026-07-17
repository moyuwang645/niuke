package com.nowcoder.community;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.nowcoder.community.util.MailClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MailClientSmtpIntegrationTest {
    private GreenMail greenMail;
    private MailClient mailClient;

    @Before
    public void setUp() {
        ServerSetup smtpSetup = ServerSetupTest.SMTP.createCopy().dynamicPort();
        greenMail = new GreenMail(smtpSetup);
        greenMail.start();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.setProtocol("smtp");

        mailClient = new MailClient();
        ReflectionTestUtils.setField(mailClient, "mailSender", mailSender);
        ReflectionTestUtils.setField(mailClient, "from", "no-reply@example.test");
    }

    @After
    public void tearDown() {
        greenMail.stop();
    }

    @Test
    public void sendsHtmlMailThroughLocalSmtpServer() throws Exception {
        mailClient.sendMail(
                "recipient@example.test",
                "Activation test",
                "<p>Activate account: <a href=\"http://localhost/activate/abc\">link</a></p>");

        assertTrue(greenMail.waitForIncomingEmail(5000, 1));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertEquals("Activation test", messages[0].getSubject());
        assertEquals("recipient@example.test",
                messages[0].getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("no-reply@example.test", messages[0].getFrom()[0].toString());
        assertTrue(messages[0].getContent().toString().contains("http://localhost/activate/abc"));
    }
}
