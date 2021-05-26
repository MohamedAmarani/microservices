package com.users.accountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details of a cart item")
public class CartItemDTO {
    @ApiModelProperty(notes = "Information of the cart item product")
    ProductDTO product;
    @ApiModelProperty(notes = "Quantity of the given product in the cart")
    int quantity;
    @ApiModelProperty(notes = "Unique id of the inventory attached to the cart item")
    String inventoryId;
    @ApiModelProperty(notes = "Availability of the cart item")
    boolean available = true;

    public CartItemDTO() {
    }

    public CartItemDTO(ProductDTO productDTO, int quantity, String inventoryId, boolean available) {
        this.product = productDTO;
        this.quantity = quantity;
        this.inventoryId = inventoryId;
        this.available = available;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO id) {
        this.product = product;
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
}
