package com.ecommerce.wishlistservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@ApiModel(description = "Details of a wishlist item")
public class WishlistItem {
    @ApiModelProperty(notes = "Unique id of the wishlist item")
    @Id
    String id;
    @ApiModelProperty(notes = "Unique id of the wishlist item product")
    private String productId;
    @ApiModelProperty(notes = "Unique id of the inventory on which the wishlist item was created")
    private String inventoryId;
    @ApiModelProperty(notes = "Target price for which a notification email will be sent to the user when reached")
    private double targetPrice;
    @ApiModelProperty(notes = "Creation date of the wishlist item")
    Date creationDate;

    public WishlistItem() {
    }

    public WishlistItem(String id, String productId, String inventoryId, double targetPrice, Date creationDate) {
        this.id = id;
        this.productId = productId;
        this.inventoryId = inventoryId;
        this.targetPrice = targetPrice;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
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
