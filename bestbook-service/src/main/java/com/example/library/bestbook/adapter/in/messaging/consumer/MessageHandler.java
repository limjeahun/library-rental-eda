package com.example.library.bestbook.adapter.in.messaging.consumer;

@FunctionalInterface
public interface MessageHandler {
    void handle() throws Exception;
}
