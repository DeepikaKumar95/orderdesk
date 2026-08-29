package com.orderdesk.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @Column(name = "customer_id")
    private String id;
    private String name;
    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    protected Customer() {}

    public Customer(String id, String name, BigDecimal creditLimit) {
        this.id = id; this.name = name; this.creditLimit = creditLimit;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getCreditLimit() { return creditLimit; }
}
