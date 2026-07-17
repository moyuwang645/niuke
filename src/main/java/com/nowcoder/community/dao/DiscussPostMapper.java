package com.nowcoder.community.dao;
import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> SelectDiscussPost(@Param("userId") int userId, @Param("offset") int offset,
                                         @Param("limit") int limit, @Param("orderMode") int orderMode);
    int SelectDiscussPostRows(@Param("userId") int userId);
    int insertDiscussPost(DiscussPost discussPost);
    DiscussPost SelectDiscussPostById(@Param("id") int id);
    int updateCommentCount(@Param("id") int id, @Param("commentCount") int commentCount);
    int updateType(@Param("id") int id, @Param("type") int type);
    int updateStatus(@Param("id") int id, @Param("status") int status);
    int updateScore(@Param("id") int id, @Param("score") double score);
}
