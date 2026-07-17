package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {
    @Autowired
    private MessageService messageService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @RequestMapping(path="/letter/list",method = RequestMethod.GET)
    public String letterList(Model model, Page page){
        User user=hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.selectCountConversation(user.getId()));
        List<Message> conversationList=messageService.selectConversations(
                user.getId(),page.getOffset(),page.getLimit());
        List<Map<String,Object>> conversations=new ArrayList<>();
        if(conversationList!=null){
            for(Message message:conversationList){
                Map<String,Object> Aconversation=new HashMap<>();
                Aconversation.put("conversation",message);
                Aconversation.put("LetterCount",messageService.selectCountLetters(message.getConversationId()));
                Aconversation.put("UnreadLetter",messageService.selectUnreadCount(user.getId(), message.getConversationId()));
                int target= user.getId() ==message.getFromId()? message.getToId():message.getFromId();
                Aconversation.put("target",userService.getUserByUserid(target));
                conversations.add(Aconversation);
            }
        }
        model.addAttribute("conversations",conversations);
        int UnreadConversationCount= messageService.selectUnreadCount(user.getId(), null);
        model.addAttribute("UnreadConversationCount",UnreadConversationCount);
        int TotalUnreadCount=messageService.selectNoticeUnreadCount(user.getId(),null);
        model.addAttribute("TotalUnreadCount",TotalUnreadCount);
        return "site/letter";
    }
    @RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String detailletterList(@PathVariable("conversationId") String conversationId, Model model,Page page){
        User target = getLetterTarget(conversationId);
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);
        page.setRows(messageService.selectCountLetters(conversationId));
        List<Message> conversationList=messageService.selectLetters(
                conversationId, page.getOffset(), page.getLimit()
        );
        List<Map<String,Object>> conversations=new ArrayList<>();
        if(conversationList!=null){
            for(Message message:conversationList){
                Map<String,Object> Aconversation=new HashMap<>();
                Aconversation.put("conversation",message);
                Aconversation.put("Fromuser",userService.getUserByUserid(message.getFromId()));
                conversations.add(Aconversation);
            }
        }
        model.addAttribute("conversations",conversations);
        model.addAttribute("target",target);
        List<Integer> ids= getLetterIds(conversationList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "site/letter-detail";
    }
    private User getLetterTarget(String conversationId){
        User currentUser = hostHolder.getUser();
        String[] ids = conversationId.split("_", -1);
        if (currentUser == null || ids.length != 2) {
            throw new AccessDeniedException("无权查看该私信");
        }

        try {
            int firstUserId = Integer.parseInt(ids[0]);
            int secondUserId = Integer.parseInt(ids[1]);
            if (currentUser.getId() == firstUserId) {
                return userService.getUserByUserid(secondUserId);
            }
            if (currentUser.getId() == secondUserId) {
                return userService.getUserByUserid(firstUserId);
            }
        } catch (NumberFormatException ignored) {
            // Invalid conversation IDs are handled as unauthorized requests below.
        }
        throw new AccessDeniedException("无权查看该私信");
    }

    private List<Integer> getLetterIds(List<Message> conversationList){
        List<Integer> ids=new ArrayList<>();
        if(conversationList!=null){
            for(Message message:conversationList){
                if(hostHolder.getUser().getId()==message.getToId()&&message.getStatus()==0){
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    @RequestMapping(path = "/letter/send",method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content){
        User target= userService.getUserByName(toName);
        if(target==null){
            return CommunityUtil.getJSONString(1,"目标用户不存在");
        }
        Message message=new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId()<message.getToId()){
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        }else{
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(new Date());
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }
    @RequestMapping(path = "/notice/list",method = RequestMethod.GET)
    public String getnoticeList(Model model){
        User user=hostHolder.getUser();

        Message Commentmessage=messageService.selectLastNotice(user.getId(),Comment);
        Map<String,Object> CommentmessageVo=new HashMap<>();
        if(Commentmessage!=null){
            CommentmessageVo.put("message",Commentmessage);
            String content = HtmlUtils.htmlUnescape(Commentmessage.getContent());
            Map<String,Object> data= JSONObject.parseObject(content,Map.class);
            CommentmessageVo.put("user",userService.getUserByUserid((Integer)data.get("userId")));
            CommentmessageVo.put("entityType",data.get("entityType"));
            CommentmessageVo.put("entityId",data.get("entityId"));
            CommentmessageVo.put("postId",data.get("postId"));
            int count=messageService.selectNoticeCount(user.getId(),Comment);
            CommentmessageVo.put("count",count);
            int UnreadCount=messageService.selectNoticeUnreadCount(user.getId(),Comment);
            CommentmessageVo.put("UnreadCount",UnreadCount);
        }
        model.addAttribute("CommentNotice",CommentmessageVo);
        Message Likemessage=messageService.selectLastNotice(user.getId(),Like);
        Map<String,Object> LikemessageVo=new HashMap<>();
        if(Likemessage!=null){
            LikemessageVo.put("message",Likemessage);
            String content = HtmlUtils.htmlUnescape(Likemessage.getContent());
            Map<String,Object> data= JSONObject.parseObject(content,Map.class);
            LikemessageVo.put("user",userService.getUserByUserid((Integer)data.get("userId")));
            LikemessageVo.put("entityType",data.get("entityType"));
            LikemessageVo.put("entityId",data.get("entityId"));
            LikemessageVo.put("postId",data.get("postId"));
            int count=messageService.selectNoticeCount(user.getId(),Like);
            LikemessageVo.put("count",count);
            int UnreadCount=messageService.selectNoticeUnreadCount(user.getId(),Like);
            LikemessageVo.put("UnreadCount",UnreadCount);
        }
        model.addAttribute("LikeNotice",LikemessageVo);
        Message Followmessage=messageService.selectLastNotice(user.getId(),Follow);
        Map<String,Object> FollowmessageVo=new HashMap<>();
        if(Followmessage!=null){
            FollowmessageVo.put("message",Followmessage);
            String content = HtmlUtils.htmlUnescape(Followmessage.getContent());
            Map<String,Object> data= JSONObject.parseObject(content,Map.class);
            FollowmessageVo.put("user",userService.getUserByUserid((Integer)data.get("userId")));
            FollowmessageVo.put("entityType",data.get("entityType"));
            FollowmessageVo.put("entityId",data.get("entityId"));
            int count=messageService.selectNoticeCount(user.getId(),Follow);
            FollowmessageVo.put("count",count);
            int UnreadCount=messageService.selectNoticeUnreadCount(user.getId(),Follow);
            FollowmessageVo.put("UnreadCount",UnreadCount);
        }
        model.addAttribute("FollowNotice",FollowmessageVo);
        int TotalUnreadCount=messageService.selectNoticeUnreadCount(user.getId(),null);
        model.addAttribute("TotalUnreadCount",TotalUnreadCount);
        int UnreadConversationCount=messageService.selectUnreadCount(user.getId(),null);
        model.addAttribute("UnreadConversationCount",UnreadConversationCount);
        return "site/notice";
    }
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Model model, Page page){
        User user=hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.selectNoticeCount(user.getId(),topic));
        List<Message> noticeList=messageService.selectNotice(user.getId(),topic,page.getOffset(),page.getLimit());
        List<Map<String,Object>> messageVo=new ArrayList<>();
        if(noticeList!=null){
            for(Message message:noticeList){
                Map<String,Object> map=new HashMap<>();
                map.put("message",message);
                String content=HtmlUtils.htmlUnescape(message.getContent());
                Map<String,Object> data= JSONObject.parseObject(content,Map.class);
                map.put("user",userService.getUserByUserid((Integer)data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId",data.get("entityId"));
                map.put("postId",data.get("postId"));
                map.put("fromUser",userService.getUserByUserid(message.getFromId()));
                messageVo.add(map);
            }
        }
        model.addAttribute("NoticeDetail",messageVo);
        model.addAttribute("topic",topic);
        List<Integer> ids= getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "site/notice-detail";
    }
}
