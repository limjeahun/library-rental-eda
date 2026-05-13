package com.example.library.member.adapter.in.messaging.consumer;

@FunctionalInterface
public interface MessageHandler {
    void handle() throws Exception;
}
