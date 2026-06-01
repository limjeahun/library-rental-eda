package com.example.library.book.adapter.out.persistence.repository;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 도서 JPA 엔티티를 저장하고 조회하는 Spring Data repository입니다.
 */
public interface BookJpaRepository extends JpaRepository<BookJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select book from BookJpaEntity book where book.no = :bookNo")
    Optional<BookJpaEntity> findByIdForUpdate(@Param("bookNo") Long bookNo);
}
