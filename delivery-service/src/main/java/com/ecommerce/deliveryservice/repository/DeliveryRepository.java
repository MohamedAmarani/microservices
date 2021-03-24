package com.ecommerce.deliveryservice.repository;


import com.ecommerce.deliveryservice.model.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends MongoRepository<Delivery, String> {
    public Optional<Delivery> findById(String id);
}

