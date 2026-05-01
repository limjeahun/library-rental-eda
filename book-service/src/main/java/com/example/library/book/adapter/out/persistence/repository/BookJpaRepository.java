package com.example.library.book.adapter.out.persistence.repository;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 도서 JPA 엔티티를 저장하고 조회하는 Spring Data repository입니다.
 */
public interface BookJpaRepository extends JpaRepository<BookJpaEntity, Long> {
}
