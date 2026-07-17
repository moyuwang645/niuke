package com.nowcoder.community.config;

import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(
                new LoginTicketFilter(userService, hostHolder),
                UsernamePasswordAuthenticationFilter.class);
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/update",
                        "/comment/add/**",
                        "/discuss/add",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                ).hasAnyAuthority(
                        AuthorityUser,
                        AuthorityAdmin,
                        AuthorityModerator
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority(
                        AuthorityModerator
                )
                .antMatchers(
                        "/discuss/delete"
                ).hasAnyAuthority(
                        AuthorityModerator,
                        AuthorityAdmin
                )
                .anyRequest().permitAll();
        http.csrf().disable();
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            httpServletResponse.setContentType("application/plain;charset=UTF-8");
                            PrintWriter out = httpServletResponse.getWriter();
                            out.write(CommunityUtil.getJSONString(403,"未登陆"));
                        }else {
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath()+"/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            httpServletResponse.setContentType("application/plain;charset=UTF-8");
                            PrintWriter out = httpServletResponse.getWriter();
                            out.write(CommunityUtil.getJSONString(403, "无权限"));
                        } else {
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/denied");
                        }
                    }
                });
        http.logout().logoutUrl("/logoutSecurity");
    }
}
