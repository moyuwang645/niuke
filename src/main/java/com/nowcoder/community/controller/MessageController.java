package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class MessageController {
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
                user.getId(),page.getOffset(),page.getRows());
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
        return "site/letter";
    }
    @RequestMapping(path = "/letter/detail/{convsersationId}",method = RequestMethod.GET)
    public String detailletterList(@PathVariable("convsersationId") String convsersationId, Model model,Page page){
        page.setLimit(5);
        page.setPath("/letter/list/"+convsersationId);
        page.setRows(messageService.selectCountLetters(convsersationId));
        List<Message> conversationList=messageService.selectLetters(
                convsersationId, page.getOffset(), page.getLimit()
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
        model.addAttribute("target",getLetterTarget(convsersationId));
        List<Integer> ids= getLetterIds(conversationList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "site/letter-detail";
    }
    private User getLetterTarget(String convsersationId){
        String[] ids=convsersationId.split("_");
        int d0=Integer.parseInt(ids[0]);
        int d1=Integer.parseInt(ids[1]);
        if(hostHolder.getUser().getId()==d0){
            return userService.getUserByUserid(d1);
        }else{
            return userService.getUserByUserid(d0);
        }
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

}
