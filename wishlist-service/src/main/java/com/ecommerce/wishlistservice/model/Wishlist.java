package com.ecommerce.wishlistservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@ApiModel(description = "Details of a wishlist")
public class Wishlist {
    @ApiModelProperty(notes = "Unique id of the discount")
    @Id
    String id;
    @ApiModelProperty(notes = "Wishlist items of the wishlist")
    List<WishlistItem> wishlistItems = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the wishlist")
    Date creationDate = new Date();

    public Wishlist() {
    }

    public Wishlist(String id, List<WishlistItem> wishlistItems, Date creationDate) {
        this.id = id;
        this.wishlistItems = wishlistItems;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<WishlistItem> getWishlistItems() {
        return wishlistItems;
    }

    public void setWishlistItems(List<WishlistItem> wishlistItems) {
        this.wishlistItems = wishlistItems;
    }

    public void addToWishlistItems(WishlistItem wishlistItem) {
        this.wishlistItems.add(wishlistItem);
    }

    public void deleteFromWishlistItems(String wishlistItemProductId) throws ResponseStatusException{
        boolean cont = true;
        for (WishlistItem wishlistItem: wishlistItems) {
            System.out.println("wishlistItemProductId: " + wishlistItemProductId + "     wishlistItem.getProductId(): " + wishlistItem.getProductId());
            if (wishlistItem.getProductId().equals(wishlistItemProductId)) {
                System.out.println("hola");
                System.out.println(wishlistItems.size());
                this.wishlistItems.remove(wishlistItem);
                System.out.println(wishlistItems.size());
                System.out.println("hola1");
                cont = false;
            }
        }
        if (cont) {
            System.out.println("cont: " + cont);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist item not found"
            );
        }
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
