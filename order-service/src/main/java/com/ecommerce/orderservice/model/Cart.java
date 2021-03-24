package com.ecommerce.orderservice.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    String id;
    List<CartItem> orderCartItems = new ArrayList<>();
    String inventoryId;

    public Cart() {
    }

    public Cart(String id, List<CartItem> cartItems, String inventoryId) {
        this.id = id;
        this.orderCartItems = cartItems;
        this.inventoryId = inventoryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CartItem> getCartItems() {
        return orderCartItems;
    }

    public void setCartItems(List<CartItem> orderCartItems) {
        this.orderCartItems = orderCartItems;
    }

    public void addCartItem(CartItem orderCartItems) {
        this.orderCartItems.add(orderCartItems);
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }
}
