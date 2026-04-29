package com.example.library.bestbook.framework.jpaadapter;

import com.example.library.bestbook.domain.model.BestBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BestBookRepository extends JpaRepository<BestBook, Long> {
}
