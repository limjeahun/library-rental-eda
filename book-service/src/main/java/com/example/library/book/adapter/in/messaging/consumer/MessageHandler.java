package com.example.library.book.adapter.in.messaging.consumer;

@FunctionalInterface
public interface MessageHandler {
    void handle() throws Exception;
}
