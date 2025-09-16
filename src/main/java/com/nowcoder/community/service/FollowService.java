package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class FollowService {
    @Autowired
    private RedisTemplate redisTemplate;
    public void Follow(int userId,int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followerKey= RedisUtil.getFollower(entityType,entityId);
                String followeeKey=RedisUtil.getFollowee(userId,entityType);
                redisOperations.multi();
                redisOperations.opsForSet().add(followerKey,userId,System.currentTimeMillis());
                redisOperations.opsForSet().add(followeeKey,entityId,System.currentTimeMillis());
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
                redisOperations.opsForSet().add(followerKey,userId);
                redisOperations.opsForSet().add(followeeKey,entityId);
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
}
