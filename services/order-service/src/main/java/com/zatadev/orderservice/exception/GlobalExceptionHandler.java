package com.zatadev.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ErrorType.NOT_FOUND.toProblemDetail(ex.getMessage());
    }

    @ExceptionHandler(OrderCancellationException.class)
    public ProblemDetail handleOrderCancellation(OrderCancellationException ex) {
        log.warn("Order cancellation error: {}", ex.getMessage());
        return ErrorType.ORDER_CANCELLATION.toProblemDetail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", details);
        return ErrorType.VALIDATION.toProblemDetail(details);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ErrorType.INTERNAL.toProblemDetail("An unexpected error occurred");
    }
}