package com.ecommerce.orderservice.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

public class InventoryItemDTO {
    @ApiModelProperty(notes = "Unique id of the inventory product")
    ProductDTO product;
    @ApiModelProperty(notes = "Unique id of the inventory catalog")
    String catalogId;
    @ApiModelProperty(notes = "Quantity of items of the inventory product")
    int quantity;
    @ApiModelProperty(notes = "Creation date of the inventory item")
    Date creationDate;

    public InventoryItemDTO() {
    }

    public InventoryItemDTO(ProductDTO product, String catalogId, int quantity, Date creationDate) {
        this.product = product;
        this.catalogId = catalogId;
        this.quantity = quantity;
        this.creationDate = creationDate;
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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

