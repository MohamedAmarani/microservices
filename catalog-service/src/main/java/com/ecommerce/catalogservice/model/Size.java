package com.ecommerce.catalogservice.model;

public class Size
{
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

