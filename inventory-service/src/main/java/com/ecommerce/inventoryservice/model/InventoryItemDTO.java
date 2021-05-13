package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

public class InventoryItemDTO {
    @ApiModelProperty(notes = "Unique id of the inventory product")
    ProductDTO product;
    @ApiModelProperty(notes = "Quantity of items of the inventory product")
    int items;

    public InventoryItemDTO() {
    }

    public InventoryItemDTO(ProductDTO product, int items) {
        this.product = product;
        this.items = items;
    }

    public ProductDTO getProductDTO() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public int getItems() {
        return items;
    }

    public void setItems(int items) {
        this.items = items;
    }
}
