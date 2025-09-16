package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;
    public void like(int userId,int entityType,int entityId,int entityuUserId){
        redisTemplate.execute(new RedisCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String likeEntityKey=RedisUtil.getEntityLike(entityType,entityId);
                String userLikeKey=RedisUtil.getUserLike(entityuUserId);
                boolean isMember=operations.opsForSet().isMember(likeEntityKey,userId);
                operations.multi();
                if(!isMember){
                    operations.opsForSet().add(userLikeKey,userId);
                    operations.opsForValue().increment(likeEntityKey,userId);
                }else {
                    operations.opsForSet().remove(userLikeKey,userId);
                    operations.opsForValue().decrement(likeEntityKey,userId);
                }
                return operations.exec();
            }
        });
    }
    public int likeCount(int entityType,int entityId){
        String likeEntityKey=RedisUtil.getEntityLike(entityType,entityId);
        return redisTemplate.opsForSet().size(likeEntityKey);
    }
    public int findLikeStatus(int userId,int entityType,int entityId){
        String likeEntityKey=RedisUtil.getEntityLike(entityType,entityId);
        return redisTemplate.opsForSet().isMember(likeEntityKey,userId)?1:0;
    }
    public int findLikeCount(int entityuUserId){
        String userLikeKey=RedisUtil.getUserLike(entityuUserId);
        Integer count=(Integer)redisTemplate.opsForValue().get(userLikeKey);
        return count==null?0:count.intValue();
    }
}
