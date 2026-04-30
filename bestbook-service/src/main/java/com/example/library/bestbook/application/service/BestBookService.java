package com.example.library.bestbook.application.service;

import com.example.library.bestbook.application.dto.BestBookResult;
import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import com.example.library.bestbook.application.port.in.BestBookQueryUseCase;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import com.example.library.bestbook.application.port.out.BestBookPort;
import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BestBookService implements BestBookQueryUseCase, RecordBestBookRentUseCase {
    private final BestBookPort bestBookPort;

    public BestBookService(BestBookPort bestBookPort) {
        this.bestBookPort = bestBookPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BestBookResult> getAllBooks() {
        return bestBookPort.findAll().stream()
            .map(BestBookResult::from)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BestBookResult> getBookById(Long id) {
        return bestBookPort.findById(id)
            .map(BestBookResult::from);
    }

    @Override
    public void recordRent(RecordBestBookRentCommand command) {
        BestBook bestBook = bestBookPort.findByItemNo(command.itemNo())
            .map(book -> {
                book.increaseBestBookCount();
                return book;
            })
            .orElseGet(() -> BestBook.registerBestBook(command.itemNo(), command.itemTitle()));
        bestBookPort.save(bestBook);
    }
}
