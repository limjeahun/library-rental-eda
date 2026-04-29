package com.example.library.bestbook.framework.web;

import com.example.library.bestbook.framework.web.dto.BestBookOutPutDTO;
import com.example.library.bestbook.framework.web.dto.BestBookRegisterDTO;
import com.example.library.bestbook.service.BestBookService;
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
    private final BestBookService bestBookService;

    public BestBookController(BestBookService bestBookService) {
        this.bestBookService = bestBookService;
    }

    @GetMapping
    public List<BestBookOutPutDTO> getAllBooks() {
        return bestBookService.getAllBooks().stream().map(BestBookOutPutDTO::from).toList();
    }

    @GetMapping("/{id}")
    public BestBookOutPutDTO getBookById(@PathVariable Long id) {
        return bestBookService.getBookById(id)
            .map(BestBookOutPutDTO::from)
            .orElseThrow(() -> new NoSuchElementException("베스트도서를 찾을 수 없습니다."));
    }

    @PostMapping
    public void registerForTest(@Valid @RequestBody BestBookRegisterDTO dto) {
        bestBookService.dealBestBook(dto.toItem());
    }
}
