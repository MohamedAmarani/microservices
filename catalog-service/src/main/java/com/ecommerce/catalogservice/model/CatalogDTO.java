package com.ecommerce.catalogservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Details of a catalog")
public class CatalogDTO {
    @ApiModelProperty(notes = "Unique id of the catalog")
    String id;
    @ApiModelProperty(notes = "Products of the catalog")
    List<ProductDTO> products = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the catalog")
    Date creationDate;

    public CatalogDTO(String id, List<ProductDTO> productDTOs, Date creationDate) {
        this.id = id;
        this.products = productDTOs;
        this.creationDate = creationDate;
    }

    public CatalogDTO(String id, Date creationDate) {
        this.id = id;
        this.creationDate = creationDate;
    }

    public CatalogDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ProductDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDTO> products) {
        this.products = products;
    }

    public void addProducts(ProductDTO product) {
        this.products.add(product);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

