package com.example.library.book.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 도서 도메인 모델의 입고와 상태 변경 규칙을 검증합니다.
 */
class BookTest {
    /**
     * 새 도서가 입고 상태로 생성되는지 검증합니다. @Test void enterBook() { Book book = Book.enterBook("도서", desc(), Classfication.LITERATURE, Location.JEONGJA); assertThat(book.getTitle()).isEqualTo("도서"); assertThat(book.getBookStatus()).isEqualTo(BookStatus.ENTERED); } 도서가 대여 가능 상태로 변경되는지 검증합니다. @Test void makeAvailable() { Book book = Book.enterBook("도서", desc(), Classfication.LITERATURE, Location.JEONGJA); book.makeAvailable(); assertThat(book.getBookStatus()).isEqualTo(BookStatus.AVAILABLE); } 도서가 대여 불가능 상태로 변경되는지 검증합니다. @Test void makeUnAvailable() { Book book = Book.enterBook("도서", desc(), Classfication.LITERATURE, Location.JEONGJA); book.makeUnAvailable(); assertThat(book.getBookStatus()).isEqualTo(BookStatus.UNAVAILABLE); } 테스트용 도서 상세 값을 생성합니다.
     *
     * @return 테스트 도서에 사용할 설명, 저자, ISBN, 출판일, 출처 값을 반환합니다.
     */
    private BookDesc desc() {
        return new BookDesc("설명", "저자", "isbn", LocalDate.now(), Source.SUPPLY);
    }
}
