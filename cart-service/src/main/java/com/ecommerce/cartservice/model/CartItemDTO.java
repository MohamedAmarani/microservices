package com.ecommerce.cartservice.model;

public class CartItemDTO {
    ProductDTO product;
    int items;
    boolean available = true;

    public CartItemDTO() {
    }

    public CartItemDTO(ProductDTO productDTO, int items, boolean available) {
        this.product = productDTO;
        this.items = items;
        this.available = available;
    }

    public ProductDTO getProductDTO() {
        return product;
    }

    public void setProductDTO(ProductDTO id) {
        this.product = product;
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
