package com.ecommerce.wishlistservice.repository;

import com.ecommerce.wishlistservice.model.Wishlist;
import com.ecommerce.wishlistservice.model.WishlistItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    public Optional<Wishlist> findById(String id);
}

