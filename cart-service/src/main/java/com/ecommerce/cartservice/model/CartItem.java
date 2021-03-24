package com.ecommerce.cartservice.model;

public class CartItem {
    String productId;
    int items;
    boolean available;

    public CartItem() {
    }

    public CartItem(String productDTO, int items, boolean available) {
        this.productId = productDTO;
        this.items = items;
        this.available = available;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
