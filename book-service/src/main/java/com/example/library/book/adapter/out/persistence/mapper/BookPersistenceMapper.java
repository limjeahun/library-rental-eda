package com.example.library.book.adapter.out.persistence.mapper;

import com.example.library.book.adapter.out.persistence.entity.BookJpaEntity;
import com.example.library.book.domain.model.Book;
import com.example.library.book.domain.model.BookDesc;
import org.springframework.stereotype.Component;

@Component
public class BookPersistenceMapper {
    public BookJpaEntity toJpaEntity(Book book) {
        return new BookJpaEntity(
            book.getNo(),
            book.getTitle(),
            book.getDesc().getDescription(),
            book.getDesc().getAuthor(),
            book.getDesc().getIsbn(),
            book.getDesc().getPublicationDate(),
            book.getDesc().getSource(),
            book.getClassfication(),
            book.getBookStatus(),
            book.getLocation()
        );
    }

    public Book toDomain(BookJpaEntity entity) {
        return new Book(
            entity.getNo(),
            entity.getTitle(),
            new BookDesc(
                entity.getDescription(),
                entity.getAuthor(),
                entity.getIsbn(),
                entity.getPublicationDate(),
                entity.getSource()
            ),
            entity.getClassfication(),
            entity.getBookStatus(),
            entity.getLocation()
        );
    }
}
