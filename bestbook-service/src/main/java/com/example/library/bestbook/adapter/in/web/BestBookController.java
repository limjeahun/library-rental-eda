package com.example.library.bestbook.adapter.in.web;

import com.example.library.bestbook.adapter.in.web.dto.BestBookRegisterRequest;
import com.example.library.bestbook.adapter.in.web.dto.BestBookResponse;
import com.example.library.bestbook.application.port.in.BestBookQueryUseCase;
import com.example.library.bestbook.application.port.in.RecordBestBookRentUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BestBookController {
    private final BestBookQueryUseCase bestBookQueryUseCase;
    private final RecordBestBookRentUseCase recordBestBookRentUseCase;

    public BestBookController(
        BestBookQueryUseCase bestBookQueryUseCase,
        RecordBestBookRentUseCase recordBestBookRentUseCase
    ) {
        this.bestBookQueryUseCase = bestBookQueryUseCase;
        this.recordBestBookRentUseCase = recordBestBookRentUseCase;
    }

    @GetMapping
    public List<BestBookResponse> getAllBooks() {
        return bestBookQueryUseCase.getAllBooks().stream()
            .map(BestBookResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public BestBookResponse getBookById(@PathVariable Long id) {
        return bestBookQueryUseCase.getBookById(id)
            .map(BestBookResponse::from)
            .orElseThrow(() -> new NoSuchElementException("베스트도서를 찾을 수 없습니다."));
    }

    @PostMapping
    public void registerForTest(@Valid @RequestBody BestBookRegisterRequest request) {
        recordBestBookRentUseCase.recordRent(request.toCommand());
    }
}
