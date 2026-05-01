package com.example.library.bestbook.application.service;

import com.example.library.bestbook.application.dto.BestBookResult;
import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;
import com.example.library.bestbook.application.port.in.BestBookQueryUseCase;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import com.example.library.bestbook.application.port.out.FindAllBestBooksPort;
import com.example.library.bestbook.application.port.out.FindBestBookByIdPort;
import com.example.library.bestbook.application.port.out.FindBestBookByItemNoPort;
import com.example.library.bestbook.application.port.out.SaveBestBookPort;
import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인기 도서 read model 조회와 대여 횟수 누적 기록 흐름을 조율하는 application service입니다.
 */
@Service
@RequiredArgsConstructor
public class BestBookService implements BestBookQueryUseCase, RecordBestBookRentUseCase {
    private final FindAllBestBooksPort findAllBestBooksPort;
    private final FindBestBookByIdPort findBestBookByIdPort;
    private final FindBestBookByItemNoPort findBestBookByItemNoPort;
    private final SaveBestBookPort saveBestBookPort;

    /**
     * 모든 인기 도서 read model을 조회합니다.
     *
     * @return 누적 대여 횟수가 기록된 인기 도서 결과 목록을 반환합니다.
     */
    @Override
    public List<BestBookResult> getAllBooks() {
        return findAllBestBooksPort.findAll().stream()
            .map(BestBookResult::from)
            .toList();
    }

    /**
     * read model 식별자로 인기 도서를 조회합니다.
     *
     * @param id 조회하거나 저장할 인기 도서 read model 식별자입니다.
     * @return 조회 결과가 있으면 인기 도서 결과를 담은 Optional을 반환합니다.
     */
    @Override
    public Optional<BestBookResult> getBookById(Long id) {
        return findBestBookByIdPort.findById(id)
            .map(BestBookResult::from);
    }

    /**
     * 도서 대여 발생을 인기 도서 read model에 반영합니다.
     *
     * @param command 집계할 도서 번호와 제목을 담은 인기 도서 기록 command입니다.
     */
    @Override
    public void recordRent(RecordBestBookRentCommand command) {
        BestBook bestBook = findBestBookByItemNoPort.findByItemNo(command.itemNo())
            .map(book -> {
                book.increaseBestBookCount();
                return book;
            })
            .orElseGet(() -> BestBook.registerBestBook(command.itemNo(), command.itemTitle()));
        saveBestBookPort.save(bestBook);
    }
}
