package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

@ApiModel(description = "Details of an order")
public class OrderDTO {
    @ApiModelProperty(notes = "Unique id of the order")
    String id;
    @ApiModelProperty(notes = "Information of the ordered cart")
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
