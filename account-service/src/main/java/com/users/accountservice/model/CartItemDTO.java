package com.users.accountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details of a cart item")
public class CartItemDTO {
    @ApiModelProperty(notes = "Information of the cart item product")
    ProductDTO product;
    @ApiModelProperty(notes = "Quantity of the given product in the cart")
    int quantity;
    @ApiModelProperty(notes = "Availability of the cart item")
    boolean available = true;

    public CartItemDTO() {
    }

    public CartItemDTO(ProductDTO productDTO, int quantity, boolean available) {
        this.product = productDTO;
        this.quantity = quantity;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
