package com.nowcoder.community.controller;

import com.nowcoder.community.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping(path = "/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Value("${community.path.upload}")
    private String updatePath;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FollowService followService;
    @LoginRequired
    @RequestMapping(value = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "site/setting";
    }
    @LoginRequired
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public String updateheader(MultipartFile headerImage, Model model){
        if(headerImage==null||headerImage.isEmpty()){
            model.addAttribute("error","未上传图片");
            return "site/setting";
        }
        String originalFilename=headerImage.getOriginalFilename();
        int extensionIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf(".");
        if(extensionIndex < 0 || extensionIndex == originalFilename.length() - 1){
            model.addAttribute("error","文件格式有误");
            return "site/setting";
        }
        String suffix=originalFilename.substring(extensionIndex).toLowerCase();
        if(!suffix.matches("\\.(png|jpe?g|gif)")){
            model.addAttribute("error","文件格式有误");
            return "site/setting";
        }
        String fileName=CommunityUtil.generateUUID()+suffix;
        File file=new File(updatePath+"/"+fileName);
        try{
            headerImage.transferTo(file);
        } catch (IOException e) {
            logger.error("文件上传失败"+e.getMessage());
            throw new RuntimeException("文件上传失败",e);
        }
        User user=hostHolder.getUser();
        String newheaderurl=domain+contextPath+"/user/header/"+fileName;
        userService.updateHesder(user.getId(),newheaderurl);
        return "redirect:/index";
    }
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        fileName = updatePath+"/"+fileName;
        String suffix=fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
        response.setContentType("image/"+("jpg".equals(suffix)?"jpeg":suffix));
        try(
            FileInputStream newfile= new FileInputStream(fileName);
            OutputStream os = response.getOutputStream();){
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b=newfile.read(buffer))!=-1) {
                os.write(buffer, 0, b);
            }
        }catch (IOException e){
            logger.error("头像读取失败"+e.getMessage());
        }
    }
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model){
        User user=userService.getUserByUserid(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);
        int likeCount=likeService.findLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        Long followeeCount=followService.followeeCount(userId,UserType);
        model.addAttribute("followeeCount",followeeCount);
        Long followerCount=followService.followerCount(UserType,userId);
        model.addAttribute("followerCount",followerCount);
        boolean isFollowed=false;
        if(hostHolder.getUser()!=null){
            isFollowed=followService.isFollow(hostHolder.getUser().getId(),UserType,userId);
        }
        model.addAttribute("isFollowed",isFollowed);
        return "site/profile";
    }
}
