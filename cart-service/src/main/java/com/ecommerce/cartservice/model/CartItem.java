package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details of a cart item")
public class CartItem {
    @ApiModelProperty(notes = "Information of the cart item product")
    String productId;
    @ApiModelProperty(notes = "Quantity of items of the product in the cart")
    int items;
    @ApiModelProperty(notes = "Availability of the cart item")
    boolean available;

    public CartItem() {
    }

    public CartItem(String productDTO, int items, boolean available) {
        this.productId = productDTO;
        this.items = items;
        this.available = available;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getItems() {
        return items;
    }

    public void setItems(int items) {
        this.items = items;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
