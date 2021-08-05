package com.ecommerce.deliveryservice.repository;


import com.ecommerce.deliveryservice.model.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends MongoRepository<Delivery, String> {
    public Optional<Delivery> findById(String id);
    public Page<Delivery> findByOrderIdContainingIgnoreCaseAndDeliveryAddressContainingIgnoreCaseAndDeliveryStateContainingIgnoreCaseAndDeliveryCompanyContainingIgnoreCaseAndCreationDateBetween(String orderId, String deliveryAddress, String deliveryState, String deliveryCompany, Date minCreationDate, Date maxCreationDate, Pageable pageable);
}

