package com.ecommerce.resourceservice.repository;

import com.ecommerce.resourceservice.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
    public Optional<Resource> findById(String id);
    public List<Resource> findByName(String name);
}

