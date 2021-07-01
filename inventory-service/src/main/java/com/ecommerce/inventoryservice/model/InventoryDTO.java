package com.ecommerce.inventoryservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Details obout an inventory")
public class InventoryDTO {
    @ApiModelProperty(notes = "Unique id of the product")
    String id;
    @ApiModelProperty(notes = "Information of the items of the inventory")
    List<InventoryItemDTO> items = new ArrayList<>();
    @ApiModelProperty(notes = "Creation date of the inventory")
    Date creationDate;

    public InventoryDTO() {
    }

    public InventoryDTO(String id, Date creationDate) {
        this.id = id;
        this.creationDate = creationDate;
    }

    public InventoryDTO(String id, List<InventoryItemDTO> items, Date creationDate) {
        this.id = id;
        this.items = items;
        this.creationDate = creationDate;
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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

