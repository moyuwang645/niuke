package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;
@Controller
public class PostScoreRefreshJob implements Job, CommunityConstant {
    private static Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticsearchService elasticsearchService;
    private static final Date epoch;
    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2025-01-01 00:00:00");
        }catch (Exception e){
            throw new RuntimeException("初始化失败",e);
        }
    }
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String rediskey= RedisUtil.getPostScore();
        BoundSetOperations boundSetOps = redisTemplate.boundSetOps(rediskey);
        if (boundSetOps.size() == 0) {
            logger.info("无变化");
            return;
        }
        logger.info("正在运算"+boundSetOps.size());
        while (boundSetOps.size() > 0) {
            this.refresh((Integer)boundSetOps.pop());
        }
        logger.info("结束运算");
    }
    private void refresh(Integer postId){
        DiscussPost post = discussPostService.getDiscussPostById(postId);
        if(post==null){
            logger.error("帖子不存在");
            return;
        }
        boolean wonderful = post.getStatus()==1;
        int commentCount = post.getCommentCount();
        long likeCount = likeService.likeCount(CommentType,postId);
        double weight = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        double score = Math.log10(Math.max(weight, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        discussPostService.updataScore(postId, score);
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
