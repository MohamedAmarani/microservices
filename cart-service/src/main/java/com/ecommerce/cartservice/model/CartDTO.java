package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "Details of a cart item")
public class CartDTO {
    @ApiModelProperty(notes = "Unique id of the cart")
    String id;
    @ApiModelProperty(notes = "Information of the cart items of the cart")
    List<CartItemDTO> cartItems = new ArrayList<>();
    @ApiModelProperty(notes = "Unique id of the inventory attached to the cart")
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

