package com.anasol.cafe.repository;

import com.anasol.cafe.entity.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemsRepository extends JpaRepository<CartItems, Long> {

    // Find items by cart ID
    List<CartItems> findByCartId(Long cartId);

    // Find specific item by cart ID and product ID
    Optional<CartItems> findByCartIdAndProductId(Long cartId, Long productId);

    // Delete item by cart ID and product ID
    @Modifying
    @Query("DELETE FROM CartItems ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    // Count items in cart
    @Query("SELECT COUNT(ci) FROM CartItems ci WHERE ci.cart.id = :cartId")
    Integer countByCartId(@Param("cartId") Long cartId);

    // Delete all items from cart
    @Modifying
    @Query("DELETE FROM CartItems ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    // Check if product exists in any cart
    @Query("SELECT COUNT(ci) > 0 FROM CartItems ci WHERE ci.product.id = :productId")
    boolean existsByProductId(@Param("productId") Long productId);
}