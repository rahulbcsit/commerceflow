package com.commerceflow.order_service.exception;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Preserve Spring's status and protocol headers while replacing unsafe exception details.
        ProblemDetail safe = problem(status, status.is5xxServerError()
                ? "An unexpected server error occurred" : "The request could not be processed");
        if (ex instanceof org.springframework.web.bind.MethodArgumentNotValidException validation) {
            safe.setProperty("errors", validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> java.util.Map.of("field", error.getField(),
                            "message", java.util.Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
                    .toList());
        }
        return super.handleExceptionInternal(ex, safe, headers, status, request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<Object> missing(OrderNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "Order not found");
    }

    @ExceptionHandler({InvalidOrderTransitionException.class, OptimisticLockingFailureException.class,
            OptimisticLockException.class})
    ResponseEntity<Object> conflict(Exception ex) {
        return response(HttpStatus.CONFLICT, "Order update conflicts with the current order state");
    }

    @ExceptionHandler(InvalidPaginationException.class)
    ResponseEntity<Object> invalid(InvalidPaginationException ex) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex) {
        LOG.error("Unexpected request failure", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }

    private static ResponseEntity<Object> response(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(problem(status, detail));
    }
}
