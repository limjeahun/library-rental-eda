package com.example.library.bestbook.application.port.in;

import com.example.library.bestbook.application.dto.RecordBestBookRentCommand;

public interface RecordBestBookRentUseCase {
    void recordRent(RecordBestBookRentCommand command);
}
