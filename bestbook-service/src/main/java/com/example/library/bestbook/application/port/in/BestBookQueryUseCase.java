package com.example.library.bestbook.application.port.in;

import com.example.library.bestbook.application.dto.BestBookResult;
import java.util.List;
import java.util.Optional;

public interface BestBookQueryUseCase {
    List<BestBookResult> getAllBooks();

    Optional<BestBookResult> getBookById(Long id);
}
