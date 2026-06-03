package com.zatadev.orderservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public enum ErrorType {

    NOT_FOUND("Resource Not Found", HttpStatus.NOT_FOUND),
    ORDER_CANCELLATION("Order Cancellation Error", HttpStatus.UNPROCESSABLE_ENTITY),
    VALIDATION("Validation Error", HttpStatus.BAD_REQUEST),
    INTERNAL("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String title;
    private final HttpStatus status;

    ErrorType(String title, HttpStatus status) {
        this.title = title;
        this.status = status;
    }

    public ProblemDetail toProblemDetail(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(this.status, detail);
        problem.setTitle(this.title);
        return problem;
    }
}