package com.ecommerce.reviewservice.model;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document
public class Review {
    @ApiModelProperty(notes = "Unique id of the order")
    @Id
    String id;
    @ApiModelProperty(notes = "Unique id of the user that made the review")
    String accountId;
    @ApiModelProperty(notes = "Unique id of the product for which the review was done")
    String productId;
    @ApiModelProperty(notes = "Comment of the user whose id is accountId to the product whose id is productId")
    String comment;
    @ApiModelProperty(notes = "Rating of the user whose id is accountId given to the product whose id is productId")
    int stars;
    @ApiModelProperty(notes = "Creation date of the review")
    Date creationDate;

    public Review() {
    }

    public Review(String id, String accountId, String productId, String comment, int stars) {
        this.id = id;
        this.accountId = accountId;
        this.productId = productId;
        this.comment = comment;
        this.stars = stars;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}