package com.ecommerce.orderservice.model;

import io.swagger.annotations.ApiModelProperty;

public class InventoryItemDTO {
    @ApiModelProperty(notes = "Unique id of the inventory product")
    ProductDTO product;
    @ApiModelProperty(notes = "Unique id of the inventory catalog")
    String catalogId;
    @ApiModelProperty(notes = "Quantity of items of the inventory product")
    int quantity;

    public InventoryItemDTO() {
    }

    public InventoryItemDTO(ProductDTO product, String catalogId, int quantity) {
        this.product = product;
        this.catalogId = catalogId;
        this.quantity = quantity;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

