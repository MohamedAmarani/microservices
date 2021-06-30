package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

import java.util.Date;

@ApiModel(description = "Details of an order")
public class OrderDTO {
    @ApiModelProperty(notes = "Unique id of the order")
    String id;
    @ApiModelProperty(notes = "Unique id of the delivery linked to the order")
    String deliveryId;
    @ApiModelProperty(notes = "Information about the ordered cart")
    CartDTO cart;
    @ApiModelProperty(notes = "Creation date of the order")
    Date creationDate;

    public OrderDTO() {
    }

    public OrderDTO(String id, CartDTO cart, String deliveryId, Date creationDate) {
        this.id = id;
        this.cart = cart;
        this.deliveryId = deliveryId;
        this.creationDate = creationDate;
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

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
