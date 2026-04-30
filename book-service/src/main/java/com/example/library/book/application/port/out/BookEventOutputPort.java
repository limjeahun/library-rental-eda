package com.example.library.book.application.port.out;

import com.example.library.common.event.EventResult;

public interface BookEventOutputPort {
    void publish(EventResult result);
}
