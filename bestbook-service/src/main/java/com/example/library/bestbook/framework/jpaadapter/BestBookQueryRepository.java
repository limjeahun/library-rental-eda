package com.example.library.bestbook.framework.jpaadapter;

import com.example.library.bestbook.domain.model.BestBook;
import com.example.library.bestbook.domain.model.QBestBook;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BestBookQueryRepository {
    private final JPAQueryFactory queryFactory;

    public BestBookQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public Optional<BestBook> findByItemNo(Long itemNo) {
        QBestBook bestBook = QBestBook.bestBook;
        return Optional.ofNullable(
            queryFactory.selectFrom(bestBook)
                .where(bestBook.itemNo.eq(itemNo))
                .fetchFirst()
        );
    }
}
