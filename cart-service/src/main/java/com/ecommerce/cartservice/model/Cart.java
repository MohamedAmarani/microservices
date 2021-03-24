package com.ecommerce.cartservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class Cart {
    @Id
    String id;
    List<CartItem> cartItems = new ArrayList<>();
    String inventoryId;

    public Cart() {
    }

    public Cart(String id, List<CartItem> cartItems, String inventoryId) {
        this.id = id;
        this.cartItems = cartItems;
        this.inventoryId = inventoryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void addCartItem(CartItem cartItems) {
        this.cartItems.add(cartItems);
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }
}
