package com.example.library.rental.adapter.in.messaging.consumer;

@FunctionalInterface
public interface MessageHandler {
    void handle() throws Exception;
}
