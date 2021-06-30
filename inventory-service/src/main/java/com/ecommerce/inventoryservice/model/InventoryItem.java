package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

public class InventoryItem {
    String productId;
    String catalogId;
    int quantity;
    @ApiModelProperty(notes = "Creation date of the inventory item")
    Date creationDate = new Date();

    public InventoryItem() {
    }

    public InventoryItem(String productId, String catalogId, int quantity, Date creationDate) {
        this.productId = productId;
        this.catalogId = catalogId;
        this.quantity = quantity;
        this.creationDate = creationDate;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    public void incrementQuantity(int quantity) throws Exception {
        if (quantity < 0)
            throw new Exception("Can not add negative stock");
        else
            this.quantity += quantity;
    }

    public void decrementQuantity(int quantity) throws Exception {
        if (this.quantity < quantity)
            throw new Exception("Fewer items than demanded");
        else
            this.quantity -= quantity;
    }
}
