package com.ecommerce.wishlistservice.repository;

import com.ecommerce.wishlistservice.model.Wishlist;
import com.ecommerce.wishlistservice.model.WishlistItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends MongoRepository<WishlistItem, String> {
    public Optional<WishlistItem> findById(String id);
    public List<WishlistItem> findByProductId(String productId);
    public List<WishlistItem> findByProductIdAndInventoryId(String productId, String inventoryId);
}

