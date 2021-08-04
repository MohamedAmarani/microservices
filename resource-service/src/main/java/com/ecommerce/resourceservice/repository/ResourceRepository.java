package com.ecommerce.resourceservice.repository;

import com.ecommerce.resourceservice.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
    public Optional<Resource> findById(String id);
    public Optional<Resource> findByName(String name);
    public Page<Resource> findByNameContainingIgnoreCaseAndDescriptionContainingIgnoreCaseAndCreationDateBetween(String name, String description, Date minCreationDate, Date maxCreationDate, Pageable pageable);
}

