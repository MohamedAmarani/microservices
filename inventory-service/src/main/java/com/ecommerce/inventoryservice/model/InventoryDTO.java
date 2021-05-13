package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "Details obout an inventory")
public class InventoryDTO {
    @ApiModelProperty(notes = "Unique id of the product")
    String id;
    @ApiModelProperty(notes = "Unique id of the inventory catalog")
    String catalogId;
    @ApiModelProperty(notes = "Information of the items of the inventory")
    List<InventoryItemDTO> items = new ArrayList<>();

    public InventoryDTO() {
    }

    public InventoryDTO(String id, String catalogId) {
        this.id = id;
        this.catalogId = catalogId;
    }

    public InventoryDTO(String id, String catalogId, List<InventoryItemDTO> items) {
        this.id = id;
        this.catalogId = catalogId;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<InventoryItemDTO> getItems() {
        return items;
    }

    public void setItems(List<InventoryItemDTO> items) {
        this.items = items;
    }

    public void addItems(InventoryItemDTO items) {
        this.items.add(items);
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }
}

