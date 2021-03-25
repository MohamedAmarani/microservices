package com.users.accountservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Account {
    @Id
    String id;
    String username;
    String email;
    String password;
    String role;
    String deliveryAddress;
    double credit;

    public Account(String username, String email, String password, String role, String deliveryAddress, double credit) {
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

    public void incrementCredit(double credit) throws Exception {
        this.credit += credit;
    }

    public void decrementCredit(double credit) throws Exception {
        if (credit <= this.credit)
            this.credit -= credit;
        else
            throw new Exception();
    }
}
