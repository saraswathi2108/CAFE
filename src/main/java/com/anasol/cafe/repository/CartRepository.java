package com.anasol.cafe.repository;

import com.anasol.cafe.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Find cart by user ID
    Optional<Cart> findByUserId(Long userId);

    // Find cart with items eagerly loaded
    @Query("SELECT DISTINCT c FROM Cart c " +
            "LEFT JOIN FETCH c.cartItems ci " +
            "LEFT JOIN FETCH ci.product p " +
            "WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    // Find cart by user ID with all relationships
    @Query("SELECT DISTINCT c FROM Cart c " +
            "LEFT JOIN FETCH c.cartItems ci " +
            "LEFT JOIN FETCH ci.product p " +
            "LEFT JOIN FETCH p.category " +
            "WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItemsAndProducts(@Param("userId") Long userId);
}