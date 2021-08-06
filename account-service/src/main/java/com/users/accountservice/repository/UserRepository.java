package com.users.accountservice.repository;

import com.users.accountservice.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<Account, String> {
    public Account findByEmail(String email);
    public Optional<Account> findByUsername(String username);
    public Page<Account> findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCaseAndRoleContainingIgnoreCaseAndDeliveryAddressContainingIgnoreCaseAndCreationDateBetween(String username, String email, String role, String deliveryAddress, Date minCreationDate, Date maxCreationDate, Pageable pageable);
}
