package com.ecommerce.catalogservice.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

public class CatalogItem {
    String productId;
    @ApiModelProperty(notes = "Creation date of the catalog item")
    Date creationDate = new Date();

    public CatalogItem() {
    }

    public CatalogItem(String productId, Date creationDate) {
        this.productId = productId;
        this.creationDate = creationDate;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
