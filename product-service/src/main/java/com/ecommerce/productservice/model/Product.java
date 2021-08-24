package com.ecommerce.productservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@ApiModel(description = "Details obout a product")
public class Product {
    @ApiModelProperty(notes = "Unique id of the product")
    @Id
    String id;
    @ApiModelProperty(notes = "Name of the product")
    @Indexed(unique = true)
    String name;
    @ApiModelProperty(notes = "Description of the product")
    String description;
    @ApiModelProperty(notes = "Name of the color of the product")
    String color;
    @ApiModelProperty(notes = "Pictures of the product")
    @NotNull(message = "At least one picture is required")
    List<Picture> pictures = new ArrayList<>();
    @ApiModelProperty(notes = "Original price of the product")
    double originalPrice;
    @ApiModelProperty(notes = "Current price of the product")
    double currentPrice;
    @ApiModelProperty(notes = "Size of the product")
    @NotNull(message = "Size is required")
    Size size;
    @ApiModelProperty(notes = "Type of the product")
    Type type;
    @ApiModelProperty(notes = "Target sex of the product")
    Sex sex;
    @ApiModelProperty(notes = "Number of sales of the product")
    int sales;
    @ApiModelProperty(notes = "Number of reviews of the product")
    int reviews;
    @ApiModelProperty(notes = "Average score of the product")
    double averageScore;
    @ApiModelProperty(notes = "Creation date of the product")
    Date creationDate;

    public Product() {
    }

    public Product(String id, String name, String description, String color, double originalPrice, double currentPrice, List<Picture> pictures,
                   Size size, Type type, Sex sex, int sales, int reviews, double averageScore, Date creationDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
        this.pictures = pictures;
        this.size = size;
        this.type = type;
        this.sex = sex;
        this.sales = sales;
        this.reviews = reviews;
        this.averageScore = averageScore;
        this.creationDate = creationDate;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
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

    public void setSizeFromString(String size) {
        this.size = Size.valueOf(size);
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

    public void setTypeFromString(String type) {
        this.type = Type.valueOf(type);
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public void setSexFromString(String sex) {
        this.sex = Sex.valueOf(sex);
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public void increaseSales(int quantity) {
        this.sales += quantity;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public int getReviews() {
        return reviews;
    }

    public void setReviews(int reviews) {
        this.reviews = reviews;
    }

    public void updateAverageScore(int newScore) {
        this.reviews += 1;
        double a = 1 /(double) reviews;
        double b = 1 - a;
        this.averageScore = (a * newScore) + (b * averageScore);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}

@ApiModel(description = "Details obout the possible sizes")
enum Size {
    S, M, L, XL, XXL
}

@ApiModel(description = "Details obout the possible product types")
enum Type {
    Shirt, Trousers, Socks, Shoes, Hat
}

@ApiModel(description = "Details obout the possible product types")
enum Sex {
    Male, Female, Unisex
}