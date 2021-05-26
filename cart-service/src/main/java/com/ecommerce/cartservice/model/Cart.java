package com.ecommerce.cartservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Document
@ApiModel(description = "Details of a cart item")
public class Cart {
    @ApiModelProperty(notes = "Unique id of the cart")
    @Id
    String id;
    @ApiModelProperty(notes = "Information of the cart items of the cart")
    List<CartItem> cartItems = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the cart item")
    Date creationDate = new Date();

    public Cart() {
    }

    public Cart(String id, List<CartItem> cartItems, Date creationDate) {
        this.id = id;
        this.cartItems = cartItems;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void addCartItem(CartItem cartItems) {
        this.cartItems.add(cartItems);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void deleteFromCartItems(String wishlistItemProductId) throws ResponseStatusException {
        boolean cont = true;
        Iterator<CartItem> iter = this.cartItems.iterator();
        while (iter.hasNext() && cont) {
            if (iter.next().getProductId().equals(wishlistItemProductId)) {
                iter.remove();
                cont = false;
            }
        }
        if (cont) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist item not found"
            );
        }
    }
}
