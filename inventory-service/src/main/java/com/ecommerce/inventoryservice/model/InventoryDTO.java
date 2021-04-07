package com.ecommerce.inventoryservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

public class InventoryDTO {
    String id;
    String catalogId;
    List<InventoryItemDTO> inventoryItems = new ArrayList<>();

    public InventoryDTO() {
    }

    public InventoryDTO(String id, String catalogId) {
        this.id = id;
        this.catalogId = catalogId;
    }

    public InventoryDTO(String id, String catalogId, List<InventoryItemDTO> inventoryItems) {
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

    public List<InventoryItemDTO> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryItemDTO> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public void addInventoryItems(InventoryItemDTO inventoryItems) {
        this.inventoryItems.add(inventoryItems);
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }
}

