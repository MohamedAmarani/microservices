package com.ecommerce.orderservice.model;

public class CartItem {
    String productId;
    int items;

    public CartItem() {
    }

    public CartItem(String productDTO, int items) {
        this.productId = productDTO;
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

}