package com.ecommerce.catalogservice.model;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Catalog {
    @Id
    String id;
    List<CatalogItem> catalogItems = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the catalog")
    Date creationDate = new Date();

    public Catalog(String id, List<CatalogItem> catalogItems, Date creationDate) {
        this.id = id;
        this.catalogItems = catalogItems;
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

    public List<CatalogItem> getCatalogItems() {
        return catalogItems;
    }

    public void setCatalogItems(List<CatalogItem> productIdentifiers) {
        this.catalogItems = catalogItems;
    }

    public void addCatalogItem(CatalogItem catalogItem) {
        this.catalogItems.add(catalogItem);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
