package com.example.library.bestbook.application.port.out;

import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;
import java.util.Optional;

public interface BestBookPort {
    List<BestBook> findAll();

    Optional<BestBook> findById(Long id);

    Optional<BestBook> findByItemNo(Long itemNo);

    BestBook save(BestBook bestBook);
}
