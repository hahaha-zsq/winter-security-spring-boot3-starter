package com.zsq.winter.security.config;

import com.zsq.winter.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Security配置类
 * 
 * 核心功能：
 * 1. 配置JWT无状态认证
 * 2. 启用方法级权限校验（@PreAuthorize、@Secured、@RolesAllowed）
 * 3. 集成自定义JWT认证过滤器
 * 4. 配置URL白名单
 *
 * @author dandandiaoming
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityProperties securityProperties;
    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    /**
     * 配置HTTP安全策略
     * 
     * 主要配置项：
     * 1. 禁用CSRF保护 - 适用于无状态的REST API
     * 2. 设置无状态会话管理 - 不创建HttpSession
     * 3. 配置请求授权规则 - 白名单路径允许匿名访问，其他需要认证
     * 4. 禁用默认的表单登录和HTTP Basic认证
     * 5. 添加自定义JWT认证过滤器到过滤器链中
     * 
     * @param http HTTP安全配置对象
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF - 适用于无状态API
                .csrf(AbstractHttpConfigurer::disable)
                // 无状态Session管理
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 白名单URL不需要认证
                        .requestMatchers(securityProperties.getWhitelist().getUrls().toArray(new String[0])).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 禁用默认登录页
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用HTTP Basic认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // === 异常处理委托 ===
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 认证失败处理：委托给 HandlerExceptionResolver
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            // 将异常转发给全局异常处理器
            handlerExceptionResolver.resolveException(request, response, null, authException);
        };
    }

    /**
     * 权限不足处理：委托给 HandlerExceptionResolver
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            // 将异常转发给全局异常处理器
            handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
        };
    }
}
