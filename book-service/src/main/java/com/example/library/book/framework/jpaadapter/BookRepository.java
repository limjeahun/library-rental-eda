package com.example.library.book.framework.jpaadapter;

import com.example.library.book.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
