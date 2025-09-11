package com.nowcoder.community.service;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper disscussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    public List<DiscussPost> getDisscussPostMapper(int userId, int offset, int limit) {
        return disscussPostMapper.SelectDiscussPost(userId,offset,limit);
    }
    public int getDisscussPostMapperCount(int userId) {
        return disscussPostMapper.SelectDiscussPostRows(userId);
    }
    public int addDiscussPost(DiscussPost post){
        if(post==null){
            throw new IllegalArgumentException("帖子不能未空");
        }
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        return disscussPostMapper.insertDiscussPost(post);
    }
    public DiscussPost getDiscussPostById(int id){
        return disscussPostMapper.SelectDiscussPostById(id);
    }
}
