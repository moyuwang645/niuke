package com.nowcoder.community.util;

public class RedisUtil {
    private static final String split=":";
    private static final String REWFIX_ENTITY_LIKE="like:entity:";
    public static String getEntityLike(int entityType,int entityId){
        return REWFIX_ENTITY_LIKE+split=entityType+split+entityId;
    }
}
