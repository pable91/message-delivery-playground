package com.deliveryplayground.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private int quantity;
    private int totalPrice;
    private LocalDateTime createdAt;

    protected Order() {
    }

    public Order(String itemName, int quantity, int totalPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
