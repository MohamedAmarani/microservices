package com.ecommerce.reviewservice.repository;

import com.ecommerce.reviewservice.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    public Optional<Review> findById(String email);
    public Page<Review> findByCreationDateBetween(Date minCreationDate, Date maxCreationDate, Pageable pageable);
}

