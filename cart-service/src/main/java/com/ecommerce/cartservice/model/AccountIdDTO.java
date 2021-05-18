package com.ecommerce.cartservice.model;

public class AccountIdDTO {
    private String accountId;

    public AccountIdDTO() {
    }

    public AccountIdDTO(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}