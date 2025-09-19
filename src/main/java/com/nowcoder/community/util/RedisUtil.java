package com.nowcoder.community.util;

public class RedisUtil {
    private static final String split=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity:";
    private static final String PREFIX_USER_LIKE="like:user:";
    private static final String PREFIX_FOLLOWER="Follower:";
    private static final String PREFIX_FOLLOWEE="Followee:";
    public static String getEntityLike(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE+split+entityType+split+entityId;
    }
    public static String getUserLike(int userId){
        return PREFIX_USER_LIKE+split+userId;
    }
    public static String getFollowee(int userId, int entityType){
        return PREFIX_FOLLOWEE+split+entityType+split+entityType;
    }
    public static String getFollower(int entityType, int entityId){
        return PREFIX_FOLLOWER+split+entityType+split+entityId;
    }
}
