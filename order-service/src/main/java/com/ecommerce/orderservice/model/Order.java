package com.ecommerce.orderservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@ApiModel(description = "Details obout an order")
public class Order {
    @ApiModelProperty(notes = "Unique id of the order")
    @Id
    String id;
    @ApiModelProperty(notes = "Unique id of the delivery linked to the order")
    String deliveryId;
    @ApiModelProperty(notes = "Information about the ordered cart")
    Cart cart;

    public Order() {
    }

    public Order(Cart cart) {
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

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }
}
