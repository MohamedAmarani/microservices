package com.ecommerce.inventoryservice.model;

public class InventoryItem {
    String productId;
    int items;

    public InventoryItem() {
    }

    public InventoryItem(String productId, int items) {
        this.productId = productId;
        this.items = items;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getItems() {
        return items;
    }

    public void setItems(int items) {
        this.items = items;
    }

    public void incrementItems(int items) throws Exception {
        if (items < 0)
            throw new Exception("Can not add negative stock");
        else
            this.items += items;
    }

    public void decrementItems(int items) throws Exception {
        if (this.items < items)
            throw new Exception("Fewer items than demanded");
        else
            this.items -= items;
    }
}
