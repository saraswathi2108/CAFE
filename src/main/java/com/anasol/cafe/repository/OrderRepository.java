package com.anasol.cafe.repository;

import com.anasol.cafe.entity.Order;
import com.anasol.cafe.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find orders by user ID using the relationship
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    List<Order> findByUserId(Long userId);

    // Get all orders with user and branch data - WITH PAGINATION
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.branch " +
            "ORDER BY o.createdAt DESC")
    List<Order> findAllWithUserAndBranch();

    // Paginated version
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.branch " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findAllWithUserAndBranch(Pageable pageable);

    // Find orders by user ID with user and branch data - WITH PAGINATION
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE u.id = :userId " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithBranch(Long userId);

    // Paginated version
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE u.id = :userId " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdWithBranch(@Param("userId") Long userId, Pageable pageable);

    // Find order by ID with all relations
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.branch " +
            "LEFT JOIN FETCH o.product " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithAllRelations(@Param("orderId") Long orderId);

    // Find by user ID (Spring Data JPA method)
    List<Order> findByUser_Id(Long userId);

    // Find by status
    List<Order> findByStatus(OrderStatus orderStatus);

    // Find orders by status with user and branch data - WITH PAGINATION
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE o.status = :status " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByStatusWithUserAndBranch(OrderStatus status);

    // Paginated version
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE o.status = :status " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByStatusWithUserAndBranch(@Param("status") OrderStatus status, Pageable pageable);

    // Find orders by user ID and status with user and branch data - WITH PAGINATION
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE u.id = :userId AND o.status = :status " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusWithBranch(@Param("userId") Long userId,
                                                @Param("status") OrderStatus status);

    // Paginated version
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE u.id = :userId AND o.status = :status " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdAndStatusWithBranch(@Param("userId") Long userId,
                                                @Param("status") OrderStatus status,
                                                Pageable pageable);
}