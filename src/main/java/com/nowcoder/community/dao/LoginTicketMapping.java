package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LoginTicketMapping {
    @Insert({
            "insert into login_ticket (user_id,ticket,status,expired) " ,
             "values (#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true,keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);
    @Select({"select id,user_id,ticket,status,expired",
             "from login_ticket where ticket=#{ticket}"})
    LoginTicket selectLoginTicket(String ticket);
    @Update({"update login_ticket set status=#{status} where ticket=#{ticket}"})
    int updateLoginTicket(String ticket, int status);

}
