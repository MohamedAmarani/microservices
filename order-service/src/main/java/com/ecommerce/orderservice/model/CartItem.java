package com.ecommerce.orderservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details obout a cart item")
public class CartItem {
    @ApiModelProperty(notes = "Unique id of the cart item product")
    String productId;
    @ApiModelProperty(notes = "Quantity of items of the cart item")
    int items;

    public CartItem() {
    }

    public CartItem(String productDTO, int items) {
        this.productId = productDTO;
        this.items = items;

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

}