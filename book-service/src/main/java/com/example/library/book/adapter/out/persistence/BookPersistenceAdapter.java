package com.example.library.book.adapter.out.persistence;

import com.example.library.book.adapter.out.persistence.mapper.BookPersistenceMapper;
import com.example.library.book.adapter.out.persistence.repository.BookJpaRepository;
import com.example.library.book.application.port.out.LoadBookPort;
import com.example.library.book.application.port.out.SaveBookPort;
import com.example.library.book.domain.model.Book;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도서 도메인 모델을 JPA 엔티티로 저장하고 도서 번호로 다시 도메인 모델을 복원하는 영속성 컴포넌트입니다.
 */
@Repository
@RequiredArgsConstructor
public class BookPersistenceAdapter implements LoadBookPort, SaveBookPort {
    private final BookJpaRepository repository;
    private final BookPersistenceMapper mapper;

    /**
     * 도메인 모델을 JPA 엔티티로 변환해 저장한 뒤 다시 도메인 모델로 반환합니다.
     *
     * @param book 저장하거나 응답 DTO로 변환할 도서 도메인 모델입니다.
     * @return 저장 후 식별자와 최신 상태가 반영된 도메인 모델을 반환합니다.
     */
    @Override
    public Book save(Book book) {
        return mapper.toDomain(repository.save(mapper.toJpaEntity(book)));
    }

    /**
     * 도서 번호로 JPA 엔티티를 조회하고 도메인 모델로 변환합니다.
     *
     * @param bookNo 조회하거나 상태를 변경할 도서 번호입니다.
     * @return 도서 번호에 해당하는 도서 도메인 모델을 반환합니다.
     */
    @Override
    public Book loadBook(long bookNo) {
        return repository.findById(bookNo)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NoSuchElementException("도서를 찾을 수 없습니다: " + bookNo));
    }
}
