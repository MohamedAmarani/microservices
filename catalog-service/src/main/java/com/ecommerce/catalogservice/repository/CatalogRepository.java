package com.ecommerce.catalogservice.repository;

import com.ecommerce.catalogservice.model.Catalog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CatalogRepository extends MongoRepository<Catalog, String> {
    public Optional<Catalog> findById(String email);
}

