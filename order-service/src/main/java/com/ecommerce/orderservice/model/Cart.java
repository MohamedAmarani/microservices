package com.ecommerce.orderservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Details of a cart item")
public class Cart {
    @ApiModelProperty(notes = "Unique id of the cart")
    @Id
    String id;
    @ApiModelProperty(notes = "Information of the cart items of the cart")
    List<CartItem> cartItems = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the cart")
    Date creationDate;

    public Cart() {
    }

    public Cart(String id, List<CartItem> cartItems, Date creationDate) {
        this.id = id;
        this.cartItems = cartItems;
        this.creationDate = creationDate;
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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
