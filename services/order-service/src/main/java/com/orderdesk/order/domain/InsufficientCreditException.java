package com.orderdesk.order.domain;

public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(String customerId) {
        super("Order exceeds credit limit for customer " + customerId);
    }
}
