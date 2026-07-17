package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {
    private static Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private HostHolder hostHolder;

    @KafkaListener(topics={Comment,Follow,Like}, groupId = "community-notice-consumer")
    public void handleMessage(ConsumerRecord record) {
        if(record==null||record.value()==null){
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("格式错误");
            return;
        }
        Message message = new Message();
        message.setCreateTime(new Date());
        message.setFromId(SystemId);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        Map<String,Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());
        if(!event.getMap().isEmpty()){
            for(Map.Entry<String,Object> entry:event.getMap().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }
    @KafkaListener(topics = {Publish}, groupId = "community-publish-consumer")
    public void handlePublish(ConsumerRecord record) {
        if(record==null||record.value()==null){
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("格式错误");
            return;
        }
        DiscussPost discussPost = discussPostService.getDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }

    @KafkaListener(topics = {Delete}, groupId = "community-delete-consumer")
    public void handleDelete(ConsumerRecord record) {
        if(record==null||record.value()==null){
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("格式错误");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
}
