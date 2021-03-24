package com.ecommerce.catalogservice.model;

import java.util.ArrayList;
import java.util.List;

public class CatalogDTO {
    String id;
    List<ProductDTO> products = new ArrayList<>();

    public CatalogDTO(String id, List<ProductDTO> productDTOs) {
        this.id = id;
        this.products = productDTOs;
    }

    public CatalogDTO(String id) {
        this.id = id;
    }

    public CatalogDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ProductDTO> getProductDTOs() {
        return products;
    }

    public void setProductDTOs(List<ProductDTO> productDTOs) {
        this.products = productDTOs;
    }

    public void addProductDTO(ProductDTO productDTOs) {
        this.products.add(productDTOs);
    }
}

