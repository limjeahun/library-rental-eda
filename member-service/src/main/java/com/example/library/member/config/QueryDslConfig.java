package com.example.library.member.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL JPA 쿼리 생성을 위한 Spring Bean 설정입니다.
 */
@Configuration
public class QueryDslConfig {
    /**
     * JPA EntityManager 기반 QueryDSL factory를 제공합니다.
     *
     * @param entityManager QueryDSL JPAQueryFactory 생성에 사용할 JPA EntityManager입니다.
     * @return QueryDSL 쿼리 생성을 위한 JPAQueryFactory를 반환합니다.
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
