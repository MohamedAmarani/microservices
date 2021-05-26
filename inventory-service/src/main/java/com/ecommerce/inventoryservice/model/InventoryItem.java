package com.ecommerce.inventoryservice.model;

public class InventoryItem {
    String productId;
    String catalogId;
    int quantity;

    public InventoryItem() {
    }

    public InventoryItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void incrementItems(int quantity) throws Exception {
        if (quantity < 0)
            throw new Exception("Can not add negative stock");
        else
            this.quantity += quantity;
    }

    public void decrementItems(int quantity) throws Exception {
        if (this.quantity < quantity)
            throw new Exception("Fewer items than demanded");
        else
            this.quantity -= quantity;
    }
}
