package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

@ApiModel(description = "Details of an order")
public class OrderDTO {
    @ApiModelProperty(notes = "Unique id of the order")
    String id;
    @ApiModelProperty(notes = "Unique id of the delivery linked to the order")
    String deliveryId;
    @ApiModelProperty(notes = "Information about the ordered cart")
    CartDTO cart;

    public OrderDTO() {
    }

    public OrderDTO(CartDTO cart) {
        this.cart = cart;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CartDTO getCart() {
        return cart;
    }

    public void setCart(CartDTO cart) {
        this.cart = cart;
    }
}
