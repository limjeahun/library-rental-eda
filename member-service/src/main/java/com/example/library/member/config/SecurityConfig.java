package com.example.library.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 예제 서비스의 HTTP 요청을 허용하는 Spring Security 설정입니다.
 */
@Configuration
public class SecurityConfig {
    /**
     * CSRF를 비활성화하고 모든 요청을 허용하는 필터 체인을 구성합니다.
     *
     * @param http 보안 필터 체인을 구성할 HttpSecurity 객체입니다.
     * @return 구성된 Spring Security 필터 체인을 반환합니다.
     * @throws Exception Spring Security 필터 체인 구성 중 오류가 발생할 때 전달됩니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .build();
    }
}
