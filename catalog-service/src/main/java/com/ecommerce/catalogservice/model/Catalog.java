package com.ecommerce.catalogservice.model;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

public class Catalog {
    @Id
    String id;
    List<CatalogItem> productIdentifiers = new ArrayList<>();

    public Catalog(String id, List<CatalogItem> productIdenitifiers) {
        this.id = id;
        this.productIdentifiers = productIdenitifiers;
    }

    public Catalog(String id) {
        this.id = id;
    }

    public Catalog() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CatalogItem> getProductIdentifiers() {
        return productIdentifiers;
    }

    public void setProductIdentifier(List<String> productIds) {
        this.productIdentifiers = productIdentifiers;
    }

    public void addProductIdentifier(CatalogItem productIdentifier) {
        this.productIdentifiers.add(productIdentifier);
    }
}
