package com.ecommerce.cartservice.model;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

public class CartDTO {
    String id;
    List<CartItemDTO> cartItems = new ArrayList<>();
    String inventoryId;

    public CartDTO() {
    }

    public CartDTO(String id, String inventoryId) {
        this.id = id;
        this.cartItems = new ArrayList<>();
        this.inventoryId = inventoryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CartItemDTO> getCartItemDTOs() {
        return cartItems;
    }

    public void setCartItemDTOs(List<CartItemDTO> cartItemDTOs) {
        this.cartItems = cartItemDTOs;
    }

    public void addCartItemDTOs(CartItemDTO cartItemDTO) {
        this.cartItems.add(cartItemDTO);
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }
}

