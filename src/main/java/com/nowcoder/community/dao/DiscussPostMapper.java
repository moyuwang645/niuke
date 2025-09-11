package com.nowcoder.community.dao;
import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> SelectDiscussPost(int userId,int offset,int limit);
    int SelectDiscussPostRows(@Param("userId") int userId);
    int insertDiscussPost(DiscussPost discussPost);
    DiscussPost SelectDiscussPostById(int id);

}
