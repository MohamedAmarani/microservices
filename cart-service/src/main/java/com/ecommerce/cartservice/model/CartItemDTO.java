package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details of a cart item")
public class CartItemDTO {
    @ApiModelProperty(notes = "Information of the cart item product")
    ProductDTO product;
    @ApiModelProperty(notes = "Quantity of items of the product in the cart")
    int items;
    @ApiModelProperty(notes = "Availability of the cart item")
    boolean available = true;

    public CartItemDTO() {
    }

    public CartItemDTO(ProductDTO productDTO, int items, boolean available) {
        this.product = productDTO;
        this.items = items;
        this.available = available;
    }

    public ProductDTO getProductDTO() {
        return product;
    }

    public void setProductDTO(ProductDTO id) {
        this.product = product;
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
