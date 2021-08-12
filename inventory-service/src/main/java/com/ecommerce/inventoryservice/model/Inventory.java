package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@ApiModel(description = "Details obout a review")
public class Inventory {
    @Id
    String id;
    List<InventoryItem> inventoryItems = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the inventory")
    Date creationDate = new Date();

    public Inventory() {
    }

    public Inventory(String id, List<InventoryItem> inventoryItems, Date creationDate) {
        this.id = id;
        this.inventoryItems = inventoryItems;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<InventoryItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public void addInventoryItems(InventoryItem inventoryItems) {
        this.inventoryItems.add(inventoryItems);
    }

    //public InventoryItem findInventoryItemByProductId(String productId) {
    //}

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
