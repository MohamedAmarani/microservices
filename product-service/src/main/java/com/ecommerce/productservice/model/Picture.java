package com.ecommerce.productservice.model;

import io.swagger.annotations.ApiModelProperty;

public class Picture {
    @ApiModelProperty(notes = "URL of the image of the product")
    String url;

    public Picture() {
    }

    public Picture(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
