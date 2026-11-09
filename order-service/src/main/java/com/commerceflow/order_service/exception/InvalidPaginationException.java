package com.commerceflow.order_service.exception;

public class InvalidPaginationException extends RuntimeException {
    public InvalidPaginationException() { super("Invalid pagination"); }
}
