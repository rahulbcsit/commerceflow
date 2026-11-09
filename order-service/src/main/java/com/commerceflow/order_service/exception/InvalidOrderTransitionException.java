package com.commerceflow.order_service.exception;

public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException() { super("Requested order status transition is not allowed"); }
}
