package com.users.accountservice.repository;

import com.users.accountservice.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<Account, String> {
    public Account findByEmail(String email);
    public List<Account> findByUsername(String name);
}
