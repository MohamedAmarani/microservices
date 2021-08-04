package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    public Optional<Order> findById(String id);
    public Page<Order> findByDeliveryIdContainingIgnoreCaseAndCreationDateBetween(String deliveryId, Date minCreationDate, Date maxCreationDate, Pageable pageable);
}
