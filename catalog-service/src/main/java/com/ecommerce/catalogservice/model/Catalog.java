package com.ecommerce.catalogservice.model;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Catalog {
    @Id
    String id;
    List<CatalogItem> productIdentifiers = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the catalog")
    Date creationDate = new Date();

    public Catalog(String id, List<CatalogItem> productIdenitifiers, Date creationDate) {
        this.id = id;
        this.productIdentifiers = productIdenitifiers;
        this.creationDate = creationDate;
    }

    public Catalog(String id) {
        this.id = id;
    }

    public Catalog() {
    }

    public Catalog(Date creationDate) {
        this.creationDate = creationDate;
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

    public void setProductIdentifiers(List<CatalogItem> productIdentifiers) {
        this.productIdentifiers = productIdentifiers;
    }

    public void addProductIdentifier(CatalogItem productIdentifier) {
        this.productIdentifiers.add(productIdentifier);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
