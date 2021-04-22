package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details obout a product size")
public class Size
{
    @ApiModelProperty(notes = "Size of the product")
    Sizes sizeId;

    public Size() {
    }

    public Size(Sizes sizeId) {
        this.sizeId = sizeId;
    }

    public Sizes getSizeId() {
        return sizeId;
    }

    public void setSizeId(Sizes sizeId) {
        this.sizeId = sizeId;
    }

}

enum Sizes {
    S, M, L, XL, XXL
}


