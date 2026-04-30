package com.example.library.book.adapter.out.persistence.repository;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookJpaRepository extends JpaRepository<BookJpaEntity, Long> {
}
