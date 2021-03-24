package com.ecommerce.orderservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Order {
    @Id
    String id;
    String deliveryId;
    Cart orderCart;

    public Order() {
    }

    public Order(Cart orderCart) {
        this.orderCart = orderCart;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Cart getCart() {
        return orderCart;
    }

    public void setCart(Cart orderCart) {
        this.orderCart = orderCart;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }
}
