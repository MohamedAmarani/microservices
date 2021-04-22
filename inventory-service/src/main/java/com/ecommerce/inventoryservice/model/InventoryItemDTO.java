package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details obout an inventory item")
public class InventoryItemDTO {
    @ApiModelProperty(notes = "Unique id of the inventory product")
    ProductDTO productDTO;
    @ApiModelProperty(notes = "Quantity of items of the inventory product")
    int items;

    public InventoryItemDTO() {
    }

    public InventoryItemDTO(ProductDTO productDTO, int items) {
        this.productDTO = productDTO;
        this.items = items;
    }

    public ProductDTO getProductDTO() {
        return productDTO;
    }

    public void setProductDTO(String id) {
        this.productDTO = productDTO;
    }

    public int getItems() {
        return items;
    }

    public void setItems(int items) {
        this.items = items;
    }
}
