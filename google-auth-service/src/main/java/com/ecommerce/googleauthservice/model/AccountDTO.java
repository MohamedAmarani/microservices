package com.ecommerce.googleauthservice.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Details of an account")
public class AccountDTO {
    @ApiModelProperty(notes = "Unique id of the account")
    String id;
    @ApiModelProperty(notes = "Username of the user", required = true)
    String username;
    @ApiModelProperty(notes = "Email of the user", required = true)
    String email;
    @ApiModelProperty(notes = "Password of the user", required = true)
    String password;
    @ApiModelProperty(notes = "Role of the user in the system", required = true)
    String role;
    @ApiModelProperty(notes = "Delivery address of the user")
    String deliveryAddress;
    @ApiModelProperty(notes = "Available credit of the user")
    double credit;

    public AccountDTO() {
    }

    public AccountDTO(String username, String email, String password, String role, String deliveryAddress, double credit) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.deliveryAddress = deliveryAddress;
        this.credit = credit;
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
}

