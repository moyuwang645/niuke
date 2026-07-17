package com.nowcoder.community;

import com.nowcoder.community.controller.MessageController;
import com.nowcoder.community.controller.UserController;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.HostHolder;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ControllerValidationUnitTest {
    @Test
    public void letterDetailRejectsConversationForOtherUsersBeforeQueryingMessages() {
        MessageController controller = new MessageController();
        MessageService messageService = mock(MessageService.class);
        HostHolder hostHolder = new HostHolder();
        User currentUser = new User();
        currentUser.setId(7);
        hostHolder.setUser(currentUser);
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        ReflectionTestUtils.setField(controller, "hostHolder", hostHolder);
        ReflectionTestUtils.setField(controller, "userService", mock(UserService.class));

        try {
            controller.detailletterList("8_9", new ExtendedModelMap(), new Page());
            fail("Expected an access denied error.");
        } catch (AccessDeniedException expected) {
            verifyZeroInteractions(messageService);
        } finally {
            hostHolder.removeUser();
        }
    }

    @Test
    public void avatarUploadRejectsFilenameWithoutExtension() {
        UserController controller = new UserController();
        MockMultipartFile image = new MockMultipartFile(
                "headerImage", "avatar", "image/png", new byte[]{1});
        Model model = new ExtendedModelMap();

        String view = controller.updateheader(image, model);

        assertEquals("site/setting", view);
        assertTrue(model.containsAttribute("error"));
    }
}
