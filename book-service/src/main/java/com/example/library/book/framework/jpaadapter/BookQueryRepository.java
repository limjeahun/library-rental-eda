package com.example.library.book.framework.jpaadapter;

import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookStatus;
import com.example.library.book.domain.model.QBook;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookQueryRepository {
    private final JPAQueryFactory queryFactory;

    public BookQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Optional<Book> findByNo(long no) {
        QBook book = QBook.book;
        return Optional.ofNullable(queryFactory.selectFrom(book).where(book.no.eq(no)).fetchFirst());
    }

    public List<Book> findByStatus(BookStatus status) {
        QBook book = QBook.book;
        return queryFactory.selectFrom(book).where(book.bookStatus.eq(status)).fetch();
    }
}
