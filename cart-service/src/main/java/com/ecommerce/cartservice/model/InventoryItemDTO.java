package com.ecommerce.cartservice.model;

public class InventoryItemDTO {
    ProductDTO productDTO;
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
