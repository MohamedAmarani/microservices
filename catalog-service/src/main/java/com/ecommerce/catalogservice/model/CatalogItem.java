package com.ecommerce.catalogservice.model;

public class CatalogItem {
    String productId;

    public CatalogItem() {
    }

    public CatalogItem(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
