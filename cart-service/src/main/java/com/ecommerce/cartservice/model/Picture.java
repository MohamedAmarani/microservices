package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details obout a picture")
public class Picture {
    @ApiModelProperty(notes = "URL of the resource of the product")
    String resourceId;

    public Picture() {
    }

    public Picture(String url) {
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String url) {
        this.resourceId = url;
    }
}
