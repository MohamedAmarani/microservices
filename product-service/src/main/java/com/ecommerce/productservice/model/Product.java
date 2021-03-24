package com.ecommerce.productservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Document
public class Product {
    @Id
    String id;
    @Indexed(unique = true)
    String name;
    String description;
    @NotNull(message = "At least one picture is required")
    List<Picture> pictures = new ArrayList<>();
    double price;
    @NotNull(message = "At least one size is required")
    Size size;

    public Product(String id, String name, String description, double price, List<Picture> pictures, Size size) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.pictures = pictures;
        this.size = size;
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

    public Size getSize() {
        return size;
    }

    public void setSizes(Size size) {
        this.size = size;
    }
}
