package com.example.library.bestbook;

import com.example.library.bestbook.config.KafkaConsumerProcessingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 인기 도서 서비스 Spring Boot 애플리케이션을 시작하는 진입점입니다.
 */
@SpringBootApplication
@EnableConfigurationProperties(KafkaConsumerProcessingProperties.class)
public class BestBookServiceApplication {
    /**
     * 인기 도서 서비스 애플리케이션을 실행합니다.
     *
     * @param args Spring Boot 애플리케이션 실행 시 전달되는 명령행 인자입니다.
     */
    public static void main(String[] args) {
        SpringApplication.run(BestBookServiceApplication.class, args);
    }
}
