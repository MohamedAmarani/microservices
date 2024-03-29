package com.users.accountservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@ApiModel(description = "Details of an account")
public class Account {
    @ApiModelProperty(notes = "Unique id of the account")
    @Id
    String id;
    @ApiModelProperty(notes = "Username of the user", required = true)
    @Indexed(unique=true)
    String username;
    @ApiModelProperty(notes = "Email of the user", required = true)
    @Indexed(unique=true)
    String email;
    @ApiModelProperty(notes = "Password of the user", required = true)
    String password;
    @ApiModelProperty(notes = "Role of the user in the system", required = true)
    String role;
    @ApiModelProperty(notes = "Delivery address of the user")
    String deliveryAddress;
    @ApiModelProperty(notes = "Available credit of the user")
    double credit;
    @ApiModelProperty(notes = "Creation date of the account")
    Date creationDate = new Date();

    public Account(String username, String email, String password, String role, String deliveryAddress, double credit, Date creationDate) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.deliveryAddress = deliveryAddress;
        this.credit = credit;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public double getCredit() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public void incrementCredit(double credit) throws Exception {
        this.credit += credit;
    }

    public void decrementCredit(double credit) throws Exception {
        if (credit <= this.credit)
            this.credit -= credit;
        else
            throw new Exception();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
