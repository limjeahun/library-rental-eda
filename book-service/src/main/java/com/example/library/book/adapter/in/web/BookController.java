package com.example.library.book.adapter.in.web;

import com.example.library.book.adapter.in.web.dto.BookRequest;
import com.example.library.book.adapter.in.web.dto.BookResponse;
import com.example.library.book.application.port.in.AddBookUseCase;
import com.example.library.book.application.port.in.BookQueryUseCase;
import com.example.library.book.application.port.in.MakeAvailableBookUseCase;
import com.example.library.book.application.port.in.MakeUnavailableBookUseCase;
import com.example.library.common.core.web.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도서 등록, 도서 조회, 대여 가능/불가능 상태 변경 HTTP 요청을 처리하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {
    private final AddBookUseCase addBookUseCase;
    private final BookQueryUseCase bookQueryUseCase;
    private final MakeAvailableBookUseCase makeAvailableBookUseCase;
    private final MakeUnavailableBookUseCase makeUnavailableBookUseCase;

    /**
     * HTTP 요청 DTO를 등록 command로 변환해 새 도서를 등록합니다.
     *
     * @param request 도서 제목, 설명, 분류, 위치를 담은 등록 요청 본문 DTO입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<BookResponse>> addBook(@Valid @RequestBody BookRequest request) {
        return BaseResponse.created(BookResponse.from(addBookUseCase.addBook(request.toCommand())))
            .toResponseEntity();
    }

    /**
     * 도서 번호로 단건 도서를 조회합니다.
     *
     * @param no 도서 번호입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @GetMapping("/{no}")
    public ResponseEntity<BaseResponse<BookResponse>> getBook(@PathVariable long no) {
        return BaseResponse.ok(BookResponse.from(bookQueryUseCase.getBook(no)))
            .toResponseEntity();
    }

    /**
     * 도서를 대여 가능 상태로 변경합니다.
     *
     * @param no 도서 번호입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @PostMapping("/{no}/available")
    public ResponseEntity<BaseResponse<BookResponse>> makeAvailable(@PathVariable long no) {
        return BaseResponse.ok(BookResponse.from(makeAvailableBookUseCase.makeAvailable(no)))
            .toResponseEntity();
    }

    /**
     * 도서를 대여 불가능 상태로 변경합니다.
     *
     * @param no 도서 번호입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @PostMapping("/{no}/unavailable")
    public ResponseEntity<BaseResponse<BookResponse>> makeUnavailable(@PathVariable long no) {
        return BaseResponse.ok(BookResponse.from(makeUnavailableBookUseCase.makeUnavailable(no)))
            .toResponseEntity();
    }
}
