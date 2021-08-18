package com.ecommerce.reviewservice.model;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    @ApiModelProperty(notes = "Number of likes given to the review by users")
    int likes;
    @ApiModelProperty(notes = "Id's of the users who liked the review")
    List<AccountIdDTO> likers = new ArrayList<>();
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

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void incrementLikes(String accountId) {
        //ver si el usuario ya le ha dado like a la review
        for (AccountIdDTO accountIdDTO: likers)
            if (accountIdDTO.getAccountId().equals(accountId))
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The account already liked this review"
                );

        likers.add(new AccountIdDTO(accountId));
        this.likes += 1;
    }

    public List<AccountIdDTO> getLikers() {
        return likers;
    }

    public void setLikers(List<AccountIdDTO> likers) {
        this.likers = likers;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}