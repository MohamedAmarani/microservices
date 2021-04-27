package com.ecommerce.deliveryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "Details obout a cart")
public class Cart {
    @ApiModelProperty(notes = "Unique id of the cart")
    String id;
    @ApiModelProperty(notes = "Information about the ordered cart items")
    List<CartItem> cartItems = new ArrayList<>();
    @ApiModelProperty(notes = "Unique id of the inventory on which the cart is linked")
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
