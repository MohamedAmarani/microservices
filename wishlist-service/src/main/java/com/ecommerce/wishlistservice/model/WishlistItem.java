package com.ecommerce.wishlistservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;

@ApiModel(description = "Details of a wishlist item")
public class WishlistItem {
    @ApiModelProperty(notes = "Unique id of the wishlist item product")
    private String productId;
    @ApiModelProperty(notes = "Target price for which a notification email will be sent to the user when reached")
    private double targetPrice;
    @ApiModelProperty(notes = "Creation date of the wishlist item")
    Date creationDate = new Date();

    public WishlistItem() {
    }

    public WishlistItem(String productId, double targetPrice, Date creationDate) {
        this.productId = productId;
        this.targetPrice = targetPrice;
        this.creationDate = creationDate;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
