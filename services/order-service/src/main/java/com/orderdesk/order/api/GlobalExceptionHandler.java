package com.orderdesk.order.api;

import com.orderdesk.order.domain.InsufficientCreditException;
import com.orderdesk.order.domain.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/** Maps exceptions to RFC 7807 problem details. Never leaks stack traces; always includes trace_id. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(), f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a)));
        return withTrace(pd);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException ex) {
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientCreditException.class)
    ProblemDetail credit(InsufficientCreditException ex) {
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException ex) {
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException ex) {
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body"));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        // Spring MVC exceptions (404 no-such-route, 405, 415, ...) implement ErrorResponse and carry their own status.
        if (ex instanceof ErrorResponse er) return withTrace(ProblemDetail.forStatusAndDetail(er.getStatusCode(), ex.getMessage()));
        log.error("Unhandled exception", ex);
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"));
    }

    private ProblemDetail withTrace(ProblemDetail pd) {
        String traceId = MDC.get("trace_id");
        if (traceId != null) pd.setProperty("traceId", traceId);
        return pd;
    }
}
