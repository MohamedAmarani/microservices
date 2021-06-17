package com.ecommerce.catalogservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "Details obout a product")
public class ProductDTO {
    @ApiModelProperty(notes = "Unique id of the product")
    @Id
    String id;
    @ApiModelProperty(notes = "Name of the product")
    @Indexed(unique = true)
    String name;
    @ApiModelProperty(notes = "Description of the product")
    String description;
    @ApiModelProperty(notes = "Pictures of the product")
    List<Picture> pictures = new ArrayList<>();
    @ApiModelProperty(notes = "Price of the product")
    double price;
    @ApiModelProperty(notes = "Size of the product")
    Size size;
    @ApiModelProperty(notes = "Type of the product")
    Type type;
    @ApiModelProperty(notes = "Target sex of the product")
    Sex sex;

    public ProductDTO() {
    }

    public ProductDTO(String id, String name, String description, double price, List<Picture> pictures, Size size, Type type, Sex sex) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.pictures = pictures;
        this.size = size;
        this.type = type;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    public void addPicture(Picture picture){
        pictures.add(picture);
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public Size getSize() {
        return size;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }
}

@ApiModel(description = "Details obout the possible product types")
enum Type {
    Shirt, Trouser, Sock, Shoes, Hat
}

@ApiModel(description = "Details obout the possible product types")
enum Sex {
    Male, Female, Unisex
}
