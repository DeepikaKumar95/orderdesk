package com.orderdesk.inventory.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id private String sku;
    @Column(name = "on_hand") private int onHand;
    private int reserved;
    protected Inventory() {}
    public String getSku() { return sku; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public int available() { return onHand - reserved; }
}
