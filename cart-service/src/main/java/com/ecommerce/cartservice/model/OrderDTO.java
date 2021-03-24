package com.ecommerce.cartservice.model;

import org.springframework.data.annotation.Id;

public class OrderDTO {
    String id;
    Cart cart;

    public OrderDTO() {
    }

    public OrderDTO(Cart cart) {
        this.cart = cart;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }
}
