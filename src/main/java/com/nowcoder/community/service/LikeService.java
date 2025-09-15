package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;
    public void like(int userId,int entityType,int entityId){
        String likeEntityKey=RedisUtil.getEntityLike(entityType,entityId);
        boolean isMember=redisTemplate.opsForSet().isMember(likeEntityKey,userId);
        if (isMember) {
            redisTemplate.opsForSet().remove(likeEntityKey,userId);
        }else  {
            redisTemplate.opsForSet().add(likeEntityKey,userId);
        }
    }
    public int likeCount(int entityType,int entityId){
        String likeEntityKey=RedisUtil.getEntityLike(entityType,entityId);
        return redisTemplate.opsForSet().size(likeEntityKey);
    }
    public int findLikeStatus(int userId,int entityType,int entityId){
        String likeEntityKey=RedisUtil.getEntityLike(entityType,entityId);
        return redisTemplate.opsForSet().isMember(likeEntityKey,userId)?1:0;
    }
}
