package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    public Optional<Product> findById(String id);
    public Page<Product> findAll(Pageable page);
    public Page<Product> findById(String id, Pageable pageable);
    public Page<Product> findByName(String name, Pageable pageable);
    public Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    public Page<Product> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);
    public Page<Product> findByColorContainingIgnoreCase(String name, Pageable pageable);
    public Page<Product> findByOriginalPriceGreaterThanEqual(double originalPrice, Pageable pageable);
    public Page<Product> findByOriginalPriceLessThanEqual(double originalPrice, Pageable pageable);
    public Page<Product> findByCurrentPriceGreaterThanEqual(double currentPrice, Pageable pageable);
    public Page<Product> findByCurrentPriceLessThanEqual(double currentPrice, Pageable pageable);
    public Page<Product> findBySize(String size, Pageable pageable);
    public Page<Product> findByType(String type, Pageable pageable);
    public Page<Product> findBySex(String sex, Pageable pageable);
    public Page<Product> findByCreationDateGreaterThanEqual(Date creationDate, Pageable pageable);
    public Page<Product> findByCreationDateLessThanEqual(Date creationDate, Pageable pageable);
    public Page<Product> findByNameContainingIgnoreCaseAndDescriptionContainingIgnoreCaseAndColorContainingIgnoreCaseAndSizeAndTypeAndSexAndOriginalPriceBetweenAndCurrentPriceBetweenAndCreationDateBetween(String name, String description, String color, String productSize, String type, String sex, double minOriginalPrice, double maxOriginalPrice, double minCurrentPrice, double maxCurrentPrice, Date minCreationDateDate, Date maxCreationDate, Pageable pageable);
}
