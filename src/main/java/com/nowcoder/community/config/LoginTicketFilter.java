package com.nowcoder.community.config;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class LoginTicketFilter extends OncePerRequestFilter {
    private final UserService userService;
    private final HostHolder hostHolder;

    public LoginTicketFilter(UserService userService, HostHolder hostHolder) {
        this.userService = userService;
        this.hostHolder = hostHolder;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String ticket = CookieUtil.getvalue(request, "ticket");
            if (ticket != null) {
                LoginTicket loginTicket = userService.selectloginticket(ticket);
                if (loginTicket != null
                        && loginTicket.getStatus() == 0
                        && loginTicket.getExpire().after(new Date())) {
                    User user = userService.getUserByUserid(loginTicket.getUserId());
                    if (user != null) {
                        hostHolder.setUser(user);
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user, user.getPassword(), userService.getAuthorities(user.getId()));
                        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            hostHolder.removeUser();
            SecurityContextHolder.clearContext();
        }
    }
}
