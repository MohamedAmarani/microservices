package com.ecommerce.wishlistservice.repository;

import com.ecommerce.wishlistservice.model.Wishlist;
import com.ecommerce.wishlistservice.model.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    public Optional<Wishlist> findById(String id);
    public Page<Wishlist> findByCreationDateBetween(Date minCreationDate, Date maxCreationDate, Pageable pageable);
}

