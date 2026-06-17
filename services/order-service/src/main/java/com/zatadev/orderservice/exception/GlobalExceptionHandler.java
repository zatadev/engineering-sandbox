package com.zatadev.orderservice.exception;

import com.zatadev.common.exception.BaseGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(OrderCancellationException.class)
    public ProblemDetail handleOrderCancellation(OrderCancellationException ex) {
        log.warn("Order cancellation error: {}", ex.getMessage());
        return ErrorType.ORDER_CANCELLATION.toProblemDetail(ex.getMessage());
    }
}
