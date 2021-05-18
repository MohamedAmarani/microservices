package com.ecommerce.discountservice.repository;

import com.ecommerce.discountservice.model.Discount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountRepository extends MongoRepository<Discount, String> {
    public Optional<Discount> findById(String id);
    public Optional<Discount> findByCode(String code);
}

