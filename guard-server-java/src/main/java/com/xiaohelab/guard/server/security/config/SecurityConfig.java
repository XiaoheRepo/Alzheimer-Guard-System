package com.xiaohelab.guard.server.security.config;

import com.xiaohelab.guard.server.security.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置。
 * <p>
 * 策略：JWT 无状态，Session 禁用，匿名接口白名单放行。
 * HC-03 幂等、HC-04 trace_id 由过滤器层保障，不在此层处理。
 * </p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（REST + JWT 无状态模式不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用 Session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 认证接口：公开
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                // 匿名扫码入口：公开
                .requestMatchers(HttpMethod.GET, "/r/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/p/**").permitAll()
                // 匿名线索上报：公开
                .requestMatchers(HttpMethod.POST, "/api/v1/public/clues/manual-entry").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/public/clues/anonymous").permitAll()
                // Actuator 健康检查：公开
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 其余全部需要认证
                .anyRequest().authenticated()
            )
            // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt 密码编码器，cost=12 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** 暴露 AuthenticationManager，供登录 UseCase 调用 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
