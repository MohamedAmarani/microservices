package com.ecommerce.inventoryservice.model;

public class InventoryItem {
    String productId;
    String catalogId;
    int quantity;

    public InventoryItem() {
    }

    public InventoryItem(String productId, String catalogId, int quantity) {
        this.productId = productId;
        this.catalogId = catalogId;
        this.quantity = quantity;
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
