package com.ecommerce.productservice.model;

import io.swagger.annotations.ApiModelProperty;

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
