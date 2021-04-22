package com.ecommerce.catalogservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "Details of a catalog")
public class CatalogDTO {
    @ApiModelProperty(notes = "Unique id of the catalog")
    String id;
    @ApiModelProperty(notes = "Products of the catalog")
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

