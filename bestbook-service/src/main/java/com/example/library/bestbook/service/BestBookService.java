package com.example.library.bestbook.service;

import com.example.library.bestbook.domain.model.BestBook;
import com.example.library.bestbook.framework.jpaadapter.BestBookQueryRepository;
import com.example.library.bestbook.framework.jpaadapter.BestBookRepository;
import com.example.library.common.vo.Item;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BestBookService {
    private final BestBookRepository bestBookRepository;
    private final BestBookQueryRepository bestBookQueryRepository;

    public BestBookService(BestBookRepository bestBookRepository, BestBookQueryRepository bestBookQueryRepository) {
        this.bestBookRepository = bestBookRepository;
        this.bestBookQueryRepository = bestBookQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<BestBook> getAllBooks() {
        return bestBookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<BestBook> getBookById(Long id) {
        return bestBookRepository.findById(id);
    }

    public void dealBestBook(Item item) {
        BestBook bestBook = bestBookQueryRepository.findByItemNo(item.getNo())
            .map(book -> {
                book.increseBestBookCount();
                return book;
            })
            .orElseGet(() -> BestBook.registerBestBook(item));
        saveBook(bestBook);
    }

    public BestBook saveBook(BestBook book) {
        return bestBookRepository.save(book);
    }
}
