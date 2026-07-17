package com.nowcoder.community;

import com.nowcoder.community.dao.LoginTicketMapping;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CoreLogicUnitTest implements CommunityConstant {
    @Mock
    private UserMapper userMapper;
    @Mock
    private MailClient mailClient;
    @Mock
    private LoginTicketMapping loginTicketMapping;
    @Mock
    private RedisTemplate redisTemplate;
    @Mock
    private RedisOperations redisOperations;
    @Mock
    private SetOperations setOperations;
    @Mock
    private ValueOperations valueOperations;
    @Mock
    private ZSetOperations zSetOperations;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private SensitiveFilter sensitiveFilter;

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService();
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(new StringTemplateResolver());
        ReflectionTestUtils.setField(userService, "userMapper", userMapper);
        ReflectionTestUtils.setField(userService, "mailClient", mailClient);
        ReflectionTestUtils.setField(userService, "templateEngine", templateEngine);
        ReflectionTestUtils.setField(userService, "loginTicketMapping", loginTicketMapping);
        ReflectionTestUtils.setField(userService, "domain", "http://localhost:8080");
        ReflectionTestUtils.setField(userService, "contextPath", "/community");
    }

    @Test
    public void registerInitializesActivationFieldsBeforeInsert() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("plain-password");
        user.setEmail("alice@example.com");
        doAnswer(invocation -> {
            User inserted = invocation.getArgument(0);
            inserted.setId(42);
            return 1;
        }).when(userMapper).insertUser(any(User.class));

        Map<String, Object> result = userService.register(user);

        assertTrue(result.isEmpty());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());
        User inserted = userCaptor.getValue();
        assertEquals(0, inserted.getStatus());
        assertEquals(0, inserted.getType());
        assertNotNull(inserted.getActivationCode());
        assertFalse(inserted.getActivationCode().isEmpty());
        assertNotNull(inserted.getCreateTime());
        assertNotEquals("plain-password", inserted.getPassword());
        verify(mailClient).sendMail(eq("alice@example.com"), anyString(), anyString());
    }

    @Test
    public void registerReportsDuplicateEmailWithTemplateKey() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("plain-password");
        user.setEmail("alice@example.com");
        when(userMapper.selectByEmail("alice@example.com")).thenReturn(new User());

        Map<String, Object> result = userService.register(user);

        assertTrue(result.containsKey("mailmsg"));
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    public void activationPersistsSuccessfulStatusChange() {
        User user = new User();
        user.setStatus(0);
        user.setActivationCode("abc");
        when(userMapper.selectById(42)).thenReturn(user);

        int result = userService.activation(42, "abc");

        assertEquals(activation_success, result);
        verify(userMapper).updateStatus(42, 1);
    }

    @Test
    public void activationReturnsFailureForUnknownUser() {
        when(userMapper.selectById(404)).thenReturn(null);

        int result = userService.activation(404, "missing");

        assertEquals(activation_fail, result);
        verify(userMapper, never()).updateStatus(anyInt(), anyInt());
    }

    @Test
    public void loginTicketExpirationUsesSeconds() {
        User user = new User();
        user.setId(7);
        user.setStatus(1);
        user.setSalt("salt");
        user.setPassword(CommunityUtil.md5("secret" + user.getSalt()));
        when(userMapper.selectByName("alice")).thenReturn(user);
        long before = System.currentTimeMillis();

        Map<String, Object> result = userService.LoginService("alice", "secret", 10);

        assertTrue(result.containsKey("ticket"));
        ArgumentCaptor<LoginTicket> ticketCaptor = ArgumentCaptor.forClass(LoginTicket.class);
        verify(loginTicketMapping).insertLoginTicket(ticketCaptor.capture());
        long expiresIn = ticketCaptor.getValue().getExpire().getTime() - before;
        assertTrue(expiresIn >= 9000L);
        assertTrue(expiresIn <= 11000L);
    }

    @Test
    public void redisFolloweeKeyIncludesUserId() {
        assertEquals("Followee:7:3", RedisUtil.getFollowee(7, 3));
    }

    @Test
    public void cookieLookupReturnsNullWhenRequestHasNoCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(CookieUtil.getvalue(request, "ticket"));
    }

    @Test
    public void likeWritesEntitySetAndUserCounter() {
        LikeService likeService = new LikeService();
        ReflectionTestUtils.setField(likeService, "redisTemplate", redisTemplate);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.opsForSet()).thenReturn(setOperations);
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(setOperations.isMember("like:entity:1:88", 7)).thenReturn(false);

        likeService.like(7, 1, 88, 99);

        verify(setOperations).add("like:entity:1:88", 7);
        verify(valueOperations).increment("like:user:99");
        verify(valueOperations, never()).increment(eq("like:entity:1:88"), anyLong());
    }

    @Test
    public void unlikeRemovesEntitySetAndDecrementsUserCounter() {
        LikeService likeService = new LikeService();
        ReflectionTestUtils.setField(likeService, "redisTemplate", redisTemplate);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.opsForSet()).thenReturn(setOperations);
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(setOperations.isMember("like:entity:1:88", 7)).thenReturn(true);

        likeService.like(7, 1, 88, 99);

        verify(setOperations).remove("like:entity:1:88", 7);
        verify(valueOperations).decrement("like:user:99");
    }

    @Test
    public void followUsesSortedSetsForBothDirections() {
        FollowService followService = new FollowService();
        ReflectionTestUtils.setField(followService, "redisTemplate", redisTemplate);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.opsForZSet()).thenReturn(zSetOperations);

        followService.Follow(7, 3, 9);

        verify(zSetOperations).add(eq("Follower:3:9"), eq(7), anyDouble());
        verify(zSetOperations).add(eq("Followee:7:3"), eq(9), anyDouble());
    }

    @Test
    public void unfollowRemovesSortedSetMembers() {
        FollowService followService = new FollowService();
        ReflectionTestUtils.setField(followService, "redisTemplate", redisTemplate);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.opsForZSet()).thenReturn(zSetOperations);

        followService.UnFollow(7, 3, 9);

        verify(zSetOperations).remove("Follower:3:9", 7);
        verify(zSetOperations).remove("Followee:7:3", 9);
    }

    @Test
    public void messageContentIsEscapedBeforeInsert() {
        MessageService messageService = new MessageService();
        ReflectionTestUtils.setField(messageService, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(messageService, "sensitiveFilter", sensitiveFilter);
        when(sensitiveFilter.filter("&lt;script&gt;")).thenReturn("&lt;script&gt;");
        Message message = new Message();
        message.setContent("<script>");

        messageService.addMessage(message);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insertMessage(messageCaptor.capture());
        assertEquals("&lt;script&gt;", messageCaptor.getValue().getContent());
    }

    @Test
    public void sensitiveFilterKeepsJsonPunctuationWithoutOverrunning() {
        SensitiveFilter filter = new SensitiveFilter();

        assertEquals("{\"entityId\":1,\"map\":{}}", filter.filter("{\"entityId\":1,\"map\":{}}"));
    }
}
