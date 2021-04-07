package com.ecommerce.inventoryservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class Inventory {
    @Id
    String id;
    String catalogId;
    List<InventoryItem> inventoryItems = new ArrayList<>();

    public Inventory() {
    }

    public Inventory(String id, String catalogId, List<InventoryItem> inventoryItems) {
        this.id = id;
        this.catalogId = catalogId;
        this.inventoryItems = inventoryItems;
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

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    //public InventoryItem findInventoryItemByProductId(String productId) {
    //}
}
