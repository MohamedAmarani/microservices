package com.ecommerce.productservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details obout a size")
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

@ApiModel(description = "Details obout the possible sizes")
enum Sizes {
    S, M, L, XL, XXL
}
