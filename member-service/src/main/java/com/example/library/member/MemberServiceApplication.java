package com.example.library.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 회원 서비스 Spring Boot 애플리케이션을 시작하는 진입점입니다.
 */
@SpringBootApplication
public class MemberServiceApplication {
    /**
     * 회원 서비스 애플리케이션을 실행합니다.
     *
     * @param args Spring Boot 애플리케이션 실행 시 전달되는 명령행 인자입니다.
     */
    public static void main(String[] args) {
        SpringApplication.run(MemberServiceApplication.class, args);
    }
}
