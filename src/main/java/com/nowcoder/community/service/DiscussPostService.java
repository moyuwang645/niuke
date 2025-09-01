package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper disscussPostMapper;
    public List<DiscussPost> getDisscussPostMapper(int userId, int offset, int limit) {
        return disscussPostMapper.SelectDiscussPost(userId,offset,limit);
    }
    public int getDisscussPostMapperCount(int userId) {
        return disscussPostMapper.SelectDiscussPostRows(userId);
    }
}
