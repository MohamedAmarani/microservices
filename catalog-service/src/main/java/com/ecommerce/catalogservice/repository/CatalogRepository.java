package com.ecommerce.catalogservice.repository;

import com.ecommerce.catalogservice.model.Catalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface CatalogRepository extends MongoRepository<Catalog, String> {
    public Optional<Catalog> findById(String email);

    public Page<Catalog> findByCreationDateBetween(Date minCreationDate, Date maxCreationDate, Pageable pageable);
}

