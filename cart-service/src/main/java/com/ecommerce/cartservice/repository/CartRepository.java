package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.model.Cart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    public Optional<Cart> findById(String id);
    public Page<Cart> findByCreationDateBetween(Date minCreationDate, Date maxCreationDate, Pageable pageable);
}

