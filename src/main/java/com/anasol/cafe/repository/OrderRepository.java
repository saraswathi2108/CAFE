package com.anasol.cafe.repository;

import com.anasol.cafe.dto.BranchDeliveredSummaryDTO;
import com.anasol.cafe.dto.DeliveredOrderStatsDTO;
import com.anasol.cafe.dto.ProductDeliveredStatsDTO;
import com.anasol.cafe.entity.Order;
import com.anasol.cafe.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.branch " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithOrderItems(@Param("orderId") Long orderId);
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch b " +
            "WHERE (:orderId IS NULL OR o.id = :orderId) " +
            "AND (:branchName IS NULL OR LOWER(b.branchName) LIKE LOWER(CONCAT('%', :branchName, '%'))) " +
            "AND (:date IS NULL OR DATE(o.createdAt) = :date) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(
            @Param("orderId") Long orderId,
            @Param("branchName") String branchName,
            @Param("date") LocalDate date,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "branch", "orderItems", "orderItems.product"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    // In OrderRepository.java
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch b " +
            "WHERE (:orderId IS NULL OR o.id = :orderId) " +
            "AND (:branchName IS NULL OR LOWER(b.branchName) LIKE CONCAT('%', LOWER(CAST(:branchName AS string)), '%')) " +
            "AND (:date IS NULL OR CAST(o.createdAt AS date) = :date) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> searchAllOrders(
            @Param("orderId") Long orderId,
            @Param("branchName") String branchName,
            @Param("date") LocalDate date,
            Pageable pageable);

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



    // In OrderRepository.java, update the queries to use proper Hibernate functions:

    @Query("SELECT new com.anasol.cafe.dto.DeliveredOrderStatsDTO(" +
            "b.id, b.branchName, b.branchCode, " +
            "YEAR(o.createdAt), MONTH(o.createdAt), " +
            "CONCAT(CAST(YEAR(o.createdAt) AS string), '-', " +
            "CASE WHEN MONTH(o.createdAt) < 10 THEN CONCAT('0', CAST(MONTH(o.createdAt) AS string)) " +
            "ELSE CAST(MONTH(o.createdAt) AS string) END), " +
            "COUNT(DISTINCT o.id), " +
            "COALESCE(SUM(oi.quantity), 0)) " +
            "FROM Order o " +
            "JOIN o.branch b " +
            "LEFT JOIN o.orderItems oi " +
            "WHERE o.status = 'DELIVERED' " +
            "AND (:year IS NULL OR YEAR(o.createdAt) = :year) " +
            "AND (:month IS NULL OR MONTH(o.createdAt) = :month) " +
            "AND (:branchId IS NULL OR b.id = :branchId) " +
            "GROUP BY b.id, b.branchName, b.branchCode, YEAR(o.createdAt), MONTH(o.createdAt) " +
            "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC")
    List<DeliveredOrderStatsDTO> getDeliveredOrdersStats(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("branchId") Long branchId);

    @Query("SELECT new com.anasol.cafe.dto.ProductDeliveredStatsDTO(" +
            "p.id, p.productName, c.categoryName, " +
            "b.id, b.branchName, " +
            "YEAR(o.createdAt), MONTH(o.createdAt), " +
            "CONCAT(CAST(YEAR(o.createdAt) AS string), '-', " +
            "CASE WHEN MONTH(o.createdAt) < 10 THEN CONCAT('0', CAST(MONTH(o.createdAt) AS string)) " +
            "ELSE CAST(MONTH(o.createdAt) AS string) END), " +
            "SUM(oi.quantity)) " +
            "FROM Order o " +
            "JOIN o.branch b " +
            "JOIN o.orderItems oi " +
            "JOIN oi.product p " +
            "LEFT JOIN p.category c " +
            "WHERE o.status = 'DELIVERED' " +
            "AND (:year IS NULL OR YEAR(o.createdAt) = :year) " +
            "AND (:month IS NULL OR MONTH(o.createdAt) = :month) " +
            "AND (:branchId IS NULL OR b.id = :branchId) " +
            "AND (:productId IS NULL OR p.id = :productId) " +
            "GROUP BY p.id, p.productName, c.categoryName, b.id, b.branchName, " +
            "YEAR(o.createdAt), MONTH(o.createdAt) " +
            "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC, SUM(oi.quantity) DESC")
    List<ProductDeliveredStatsDTO> getProductDeliveredStats(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("branchId") Long branchId,
            @Param("productId") Long productId);

    @Query("SELECT new com.anasol.cafe.dto.BranchDeliveredSummaryDTO(" +
            "b.id, b.branchName, b.branchCode, " +
            "COUNT(DISTINCT o.id), " +
            "COALESCE(SUM(oi.quantity), 0)) " +
            "FROM Order o " +
            "JOIN o.branch b " +
            "LEFT JOIN o.orderItems oi " +
            "WHERE o.status = 'DELIVERED' " +
            "AND (:year IS NULL OR YEAR(o.createdAt) = :year) " +
            "GROUP BY b.id, b.branchName, b.branchCode " +
            "ORDER BY COUNT(DISTINCT o.id) DESC")
    List<BranchDeliveredSummaryDTO> getBranchDeliveredSummary(
            @Param("year") Integer year);

    // In OrderRepository.java, update the native queries with schema prefix:

    @Query(value = """
    SELECT 
        p.id as productId,
        p.product_name as productName,
        COALESCE(c.category_name, 'Uncategorized') as categoryName,
        b.id as branchId,
        b.branch_name as branchName,
        EXTRACT(YEAR FROM o.created_at) as year,
        EXTRACT(MONTH FROM o.created_at) as month,
        TO_CHAR(o.created_at, 'YYYY-MM') as monthName,
        SUM(oi.quantity) as totalQuantityDelivered
    FROM cafe.orders o
    JOIN cafe.branches b ON o.branch_id = b.id
    JOIN cafe.order_items oi ON o.id = oi.order_id
    JOIN cafe.product p ON oi.product_id = p.id
    LEFT JOIN cafe.category c ON p.category_id = c.id
    WHERE o.status = 'DELIVERED'
    AND (:year IS NULL OR EXTRACT(YEAR FROM o.created_at) = :year)
    AND (:month IS NULL OR EXTRACT(MONTH FROM o.created_at) = :month)
    AND (:branchId IS NULL OR b.id = :branchId)
    AND (:productId IS NULL OR p.id = :productId)
    GROUP BY p.id, p.product_name, c.category_name, b.id, b.branch_name,
             EXTRACT(YEAR FROM o.created_at), EXTRACT(MONTH FROM o.created_at),
             TO_CHAR(o.created_at, 'YYYY-MM')
    ORDER BY EXTRACT(YEAR FROM o.created_at) DESC, EXTRACT(MONTH FROM o.created_at) DESC,
             SUM(oi.quantity) DESC
    """, nativeQuery = true)
    List<Object[]> getProductDeliveredStatsNative(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("branchId") Long branchId,
            @Param("productId") Long productId);


    // For Admin/Godown Manager
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch b " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByDateRangeWithUserAndBranch(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // For Manager/Staff (their own orders only)
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.branch b " +
            "WHERE o.user.id = :userId " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByUserAndDateRangeWithBranch(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}