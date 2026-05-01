package com.example.library.common.core.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record BaseResponse<T>(
    int code,
    String message,
    T data
) {
    private static final String OK_MESSAGE = "요청이 정상적으로 처리되었습니다.";
    private static final String CREATED_MESSAGE = "리소스가 성공적으로 생성되었습니다.";
    private static final String ACCEPTED_MESSAGE = "리소스가 성공적으로 접수되었습니다.";

    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(HttpStatus.OK.value(), OK_MESSAGE, data);
    }

    public static <T> BaseResponse<T> created(T data) {
        return new BaseResponse<>(HttpStatus.CREATED.value(), CREATED_MESSAGE, data);
    }

    public static <T> BaseResponse<T> accepted(T data) {
        return new BaseResponse<>(HttpStatus.ACCEPTED.value(), ACCEPTED_MESSAGE, data);
    }

    public ResponseEntity<BaseResponse<T>> toResponseEntity() {
        return ResponseEntity.status(code).body(this);
    }
}
