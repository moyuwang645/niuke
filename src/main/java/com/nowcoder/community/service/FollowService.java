package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;
    public void Follow(int userId,int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followerKey= RedisUtil.getFollower(entityType,entityId);
                String followeeKey=RedisUtil.getFollowee(userId,entityType);
                redisOperations.multi();
                redisOperations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                redisOperations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                return redisOperations.exec();
            }
        });{
        }
    }
    public void UnFollow(int userId,int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followerKey= RedisUtil.getFollower(entityType,entityId);
                String followeeKey=RedisUtil.getFollowee(userId,entityType);
                redisOperations.multi();
                redisOperations.opsForZSet().remove(followerKey,userId);
                redisOperations.opsForZSet().remove(followeeKey,entityId);
                return redisOperations.exec();
            }
        });{
        }
    }
    public long followeeCount(int userId,int entityType){
        String followeeKey=RedisUtil.getFollowee(userId,entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }
    public long followerCount(int entityType,int entityId){
        String followerKey=RedisUtil.getFollower(entityType,entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    public boolean isFollow(int userId,int entityType, int entityId){
        String followeeKey=RedisUtil.getFollowee(userId,entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId) != null;
    }
    public List<Map<String,Object>> getFollowee(int userId, int offset, int limit){
        String followeeKey=RedisUtil.getFollowee(userId,UserType);
        Set<Integer> ids=redisTemplate.opsForZSet().reverseRange(followeeKey,offset,offset+limit-1);
        if(ids==null){
            return null;
        }
        List<Map<String,Object>> followeeList=new ArrayList<Map<String,Object>>();
        for(Integer id:ids){
            Map<String,Object> map=new HashMap<>();
            User user=userService.getUserByUserid(id);
            map.put("user",user);
            double score=redisTemplate.opsForZSet().score(followeeKey,id);
            map.put("followTime",new Date((long)score));
            followeeList.add(map);
        }
        return followeeList;
    }
    public List<Map<String,Object>> getFollower(int userId, int offset, int limit){
        String followerKey=RedisUtil.getFollower(UserType,userId);
        Set<Integer> ids=redisTemplate.opsForZSet().reverseRange(followerKey,offset,offset+limit-1);
        if(ids==null){
            return null;
        }
        List<Map<String,Object>> followerList=new ArrayList<Map<String,Object>>();
        for(Integer id:ids){
            Map<String,Object> map=new HashMap<>();
            User user=userService.getUserByUserid(id);
            map.put("user",user);
            double score=redisTemplate.opsForZSet().score(followerKey,id);
            map.put("followTime",new Date((long)score));
            followerList.add(map);
        }
        return followerList;
    }
}
