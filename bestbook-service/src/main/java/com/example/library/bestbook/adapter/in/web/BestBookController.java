package com.example.library.bestbook.adapter.in.web;

import com.example.library.bestbook.adapter.in.web.dto.BestBookRegisterRequest;
import com.example.library.bestbook.adapter.in.web.dto.BestBookResponse;
import com.example.library.bestbook.application.port.in.BestBookQueryUseCase;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import com.example.library.common.core.web.BaseResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인기 도서 read model 목록/단건 조회와 수동 대여 집계 테스트 요청을 처리하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BestBookController {
    private final BestBookQueryUseCase bestBookQueryUseCase;
    private final RecordBestBookRentUseCase recordBestBookRentUseCase;

    /**
     * 모든 인기 도서 read model을 조회합니다.
     *
     * @return 누적 대여 횟수가 기록된 인기 도서 결과 목록을 반환합니다.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<BestBookResponse>>> getAllBooks() {
        return BaseResponse.ok(bestBookQueryUseCase.getAllBooks().stream()
            .map(BestBookResponse::from)
            .toList()).toResponseEntity();
    }

    /**
     * read model 식별자로 인기 도서를 조회합니다.
     *
     * @param id 조회하거나 저장할 인기 도서 read model 식별자입니다.
     * @return 클라이언트에 반환할 HTTP 응답 DTO를 반환합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BestBookResponse>> getBookById(@PathVariable Long id) {
        return BaseResponse.ok(bestBookQueryUseCase.getBookById(id)
            .map(BestBookResponse::from)
            .orElseThrow(() -> new NoSuchElementException("베스트도서를 찾을 수 없습니다."))).toResponseEntity();
    }

    /**
     * 수동 테스트용 요청의 도서 번호와 제목을 인기 도서 집계에 반영합니다.
     *
     * @param request 수동 집계할 도서 번호와 제목을 담은 요청 본문 DTO입니다.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> registerForTest(@Valid @RequestBody BestBookRegisterRequest request) {
        recordBestBookRentUseCase.recordRent(request.toCommand());
        return BaseResponse.ok((Void) null).toResponseEntity();
    }
}
