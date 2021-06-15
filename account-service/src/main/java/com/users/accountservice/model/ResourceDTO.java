package com.users.accountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotNull;

@ApiModel(description = "Details obout a product")
public class ResourceDTO {
    @ApiModelProperty(notes = "Unique id of the resource")
    @Id
    String id;
    @ApiModelProperty(notes = "Name of the resource")
    @Indexed(unique = true)
    String name;
    @ApiModelProperty(notes = "Description of the product")
    String description;
    @ApiModelProperty(notes = "Data of the resource in String Base64 format")
    @NotNull(message = "Data is required")
    String data;

    public ResourceDTO() {
    }

    public ResourceDTO(String id, String name, String description, String data) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
