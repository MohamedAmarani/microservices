package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

@ApiModel(description = "Details of a cart item")
public class CartItem {
    @ApiModelProperty(notes = "Information of the cart item product")
    String productId;
    @ApiModelProperty(notes = "Quantity of the given products in the cart")
    int quantity;
    @ApiModelProperty(notes = "Unique id of the inventory attached to the cart item")
    String inventoryId;
    @ApiModelProperty(notes = "Availability of the cart item")
    boolean available;
    @ApiModelProperty(notes = "Creation date of the cart item")
    Date creationDate = new Date();

    public CartItem() {
    }

    public CartItem(String productDTO, int quantity, String inventoryId, boolean available, Date creationDate) {
        this.productId = productDTO;
        this.quantity = quantity;
        this.inventoryId = inventoryId;
        this.available = available;
        this.creationDate = creationDate;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
