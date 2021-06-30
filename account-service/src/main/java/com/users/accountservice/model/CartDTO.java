package com.users.accountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Details of a cart item")
public class CartDTO {
    @ApiModelProperty(notes = "Unique id of the cart")
    String id;
    @ApiModelProperty(notes = "Information of the cart items of the cart")
    List<CartItemDTO> items = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the cart")
    Date creationDate;

    public CartDTO() {
    }

    public CartDTO(String id, Date creationDate) {
        this.id = id;
        this.items = new ArrayList<>();
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public void addItems(CartItemDTO item) {
        this.items.add(item);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
