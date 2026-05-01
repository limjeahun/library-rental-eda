package com.example.library.common.core.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        return build(
            ErrorCode.VALIDATION_ERROR,
            request,
            toFieldErrors(ex.getBindingResult())
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        return build(
            ErrorCode.VALIDATION_ERROR,
            request,
            toFieldErrors(ex.getBindingResult())
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> new ErrorResponse.FieldError(
                violation.getPropertyPath().toString(),
                messageOrDefault(violation.getMessage(), ErrorCode.BAD_REQUEST.message())
            ))
            .toList();

        return build(ErrorCode.VALIDATION_ERROR, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        return build(ErrorCode.MALFORMED_REQUEST_BODY, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
        MissingServletRequestParameterException ex,
        HttpServletRequest request
    ) {
        return build(
            ErrorCode.MISSING_REQUEST_PARAMETER,
            ex.getParameterName() + " 요청 파라미터는 필수입니다.",
            request
        );
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariable(
        MissingPathVariableException ex,
        HttpServletRequest request
    ) {
        return build(
            ErrorCode.MISSING_PATH_VARIABLE,
            ex.getVariableName() + " 경로 변수는 필수입니다.",
            request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        String requiredType = ex.getRequiredType() == null ? "요청 타입" : ex.getRequiredType().getSimpleName();
        return build(
            ErrorCode.TYPE_MISMATCH,
            ex.getName() + " 값의 타입이 올바르지 않습니다. 기대 타입: " + requiredType,
            request,
            List.of(new ErrorResponse.FieldError(ex.getName(), "타입이 올바르지 않습니다."))
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(ErrorCode.BAD_REQUEST, messageOrDefault(ex.getMessage(), ErrorCode.BAD_REQUEST.message()), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest request) {
        return build(ErrorCode.NOT_FOUND, messageOrDefault(ex.getMessage(), ErrorCode.NOT_FOUND.message()), request);
    }

    @ExceptionHandler({
        NoHandlerFoundException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request
    ) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return build(ErrorCode.DATA_INTEGRITY_VIOLATION, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return build(ErrorCode.CONFLICT, messageOrDefault(ex.getMessage(), ErrorCode.CONFLICT.message()), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String error = status == null ? "HTTP Error" : status.getReasonPhrase();
        ErrorCode errorCode = ErrorCode.fromStatus(statusCode);
        return build(
            statusCode,
            error,
            errorCode,
            messageOrDefault(ex.getReason(), errorCode.message()),
            request,
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled web exception", ex);
        return build(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode, HttpServletRequest request) {
        return build(errorCode, errorCode.message(), request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(
        ErrorCode errorCode,
        String message,
        HttpServletRequest request
    ) {
        return build(errorCode, message, request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(
        ErrorCode errorCode,
        HttpServletRequest request,
        List<ErrorResponse.FieldError> fieldErrors
    ) {
        return build(errorCode, errorCode.message(), request, fieldErrors);
    }

    private ResponseEntity<ErrorResponse> build(
        ErrorCode errorCode,
        String message,
        HttpServletRequest request,
        List<ErrorResponse.FieldError> fieldErrors
    ) {
        return build(errorCode.status(), errorCode.status().getReasonPhrase(), errorCode, message, request, fieldErrors);
    }

    private ResponseEntity<ErrorResponse> build(
        HttpStatusCode statusCode,
        String error,
        ErrorCode errorCode,
        String message,
        HttpServletRequest request,
        List<ErrorResponse.FieldError> fieldErrors
    ) {
        return ResponseEntity.status(statusCode)
            .body(ErrorResponse.of(statusCode.value(), error, errorCode.code(), message, request.getRequestURI(), fieldErrors));
    }

    private List<ErrorResponse.FieldError> toFieldErrors(BindingResult bindingResult) {
        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
            .map(error -> new ErrorResponse.FieldError(
                error.getField(),
                messageOrDefault(error.getDefaultMessage(), ErrorCode.BAD_REQUEST.message())
            ))
            .toList();

        List<ErrorResponse.FieldError> globalErrors = bindingResult.getGlobalErrors().stream()
            .map(this::toGlobalFieldError)
            .toList();

        if (globalErrors.isEmpty()) {
            return fieldErrors;
        }

        return java.util.stream.Stream.concat(fieldErrors.stream(), globalErrors.stream()).toList();
    }

    private ErrorResponse.FieldError toGlobalFieldError(ObjectError error) {
        return new ErrorResponse.FieldError(
            error.getObjectName(),
            messageOrDefault(error.getDefaultMessage(), ErrorCode.BAD_REQUEST.message())
        );
    }

    private String messageOrDefault(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
