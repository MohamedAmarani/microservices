package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

public class InventoryItemDTO {
    @ApiModelProperty(notes = "Unique id of the inventory product")
    ProductDTO product;
    @ApiModelProperty(notes = "Quantity of items of the inventory product")
    int quantity;

    public InventoryItemDTO() {
    }

    public InventoryItemDTO(ProductDTO product, int items) {
        this.product = product;
        this.quantity = items;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
