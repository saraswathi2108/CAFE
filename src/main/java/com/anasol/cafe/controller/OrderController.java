package com.anasol.cafe.controller;

import com.anasol.cafe.dto.*;
import com.anasol.cafe.entity.OrderStatus;
import com.anasol.cafe.entity.Product;
import com.anasol.cafe.exceptions.OrderProcessingException;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.repository.ProductRepo;
import com.anasol.cafe.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cafe/orders")
@CrossOrigin(origins = "*")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ProductRepo productRepo;

    public OrderController(OrderService orderService, ProductRepo productRepo) {
        this.orderService = orderService;
        this.productRepo = productRepo;
    }

    // Existing endpoint for single product ordering
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @PostMapping("/order")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody OrderRequestDTO orderRequest) {
        return ResponseEntity.ok(orderService.placeOrder(orderRequest));
    }



    // NEW: Endpoint for cart-based ordering (multiple products)
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @PostMapping("/order/cart")
    public ResponseEntity<OrderResponseDTO> placeOrderFromCart(@RequestBody CartOrderRequestDTO cartOrderRequest) {
        return ResponseEntity.ok(orderService.placeOrderFromCart(cartOrderRequest));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getAllOrders(page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    // In OrderController.java, add these endpoints:

    // For admin to search all orders
    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/search")
    public ResponseEntity<Map<String, Object>> searchAllOrders(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.searchAllOrders(
                orderId, branchName, date, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    // For users to search their own orders
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @GetMapping("/user/search")
    public ResponseEntity<Map<String, Object>> searchMyOrders(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.searchOrders(
                orderId, branchName, date, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    // Combined search endpoint for all roles (with role-based filtering)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF','GODOWN_MANAGER')")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchOrders(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.searchOrders(
                orderId, branchName, date, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/pending")
    public ResponseEntity<Map<String, Object>> getPendingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getPendingOrders(page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/approved")
    public ResponseEntity<Map<String, Object>> getApprovedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getApprovedOrders(page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @GetMapping("/user/pending")
    public ResponseEntity<Map<String, Object>> getUserPendingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getMyOrdersByStatus(
                OrderStatus.PENDING, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @GetMapping("/user/approved")
    public ResponseEntity<Map<String, Object>> getUserApprovedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getMyOrdersByStatus(
                OrderStatus.APPROVED, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable Long orderId,
            @RequestBody StatusUpdateDTO statusUpdate) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, statusUpdate.getStatus())
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getMyOrders(page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @PutMapping("/admin/{orderId}/ship")
    public ResponseEntity<OrderResponseDTO> shipOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED)
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    @PutMapping("/user/{orderId}/received")
    public ResponseEntity<OrderResponseDTO> receiveOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<Map<String, Object>> getOrdersByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getOrdersByUser(userId, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/user/{userId}/{status}")
    public ResponseEntity<Map<String, Object>> getUserOrdersByStatus(
            @PathVariable Long userId,
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getUserOrdersByStatus(
                userId, status, page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @PutMapping("/admin/{orderId}/reject")
    public ResponseEntity<OrderResponseDTO> rejectOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, OrderStatus.REJECTED)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/rejected")
    public ResponseEntity<Map<String, Object>> getRejectedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getRejectedOrders(page, size, sortBy, direction);

        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
    @GetMapping("/{orderId}/items")
    public ResponseEntity<OrderResponseDTO> getOrderWithItems(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderWithItems(orderId));
    }

    private Map<String, Object> createPagedResponse(Page<OrderResponseDTO> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("pageSize", page.getSize());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }



    // In OrderController.java, add these endpoints:

    // Get delivered orders statistics by month and branch
    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/stats/delivered")
    public ResponseEntity<List<DeliveredOrderStatsDTO>> getDeliveredOrdersStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long branchId) {

        List<DeliveredOrderStatsDTO> stats = orderService.getDeliveredOrdersStats(year, month, branchId);
        return ResponseEntity.ok(stats);
    }

    // Get product-wise delivered statistics
    @PreAuthorize("hasRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/stats/delivered/products")
    public ResponseEntity<List<ProductDeliveredStatsDTO>> getProductDeliveredStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId) {

        List<ProductDeliveredStatsDTO> stats = orderService.getProductDeliveredStats(year, month, branchId, productId);
        return ResponseEntity.ok(stats);
    }

    // Get branch-wise delivered summary
    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/stats/delivered/branches")
    public ResponseEntity<List<BranchDeliveredSummaryDTO>> getBranchDeliveredSummary(
            @RequestParam(required = false) Integer year) {

        List<BranchDeliveredSummaryDTO> summary = orderService.getBranchDeliveredSummary(year);
        return ResponseEntity.ok(summary);
    }

    // Get monthly delivered orders for a specific branch
    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/stats/delivered/branch/{branchId}/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyDeliveredOrdersByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false) Integer year) {

        // Default to current year if not specified
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        Map<String, Object> stats = orderService.getMonthlyDeliveredOrdersByBranch(branchId, year);
        return ResponseEntity.ok(stats);
    }

    // Get top delivered products across all branches
    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/stats/delivered/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopDeliveredProducts(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> topProducts = orderService.getTopDeliveredProducts(year, month, limit);
        return ResponseEntity.ok(topProducts);
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF', 'GODOWN_MANAGER')")
    @GetMapping("/by-date")
    public ResponseEntity<Map<String, Object>> getOrdersByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<OrderResponseDTO> orderPage = orderService.getOrdersByDateRange(
                fromDate, toDate, page, size, sortBy, direction);
        Map<String, Object> response = createPagedResponse(orderPage);
        return ResponseEntity.ok(response);
    }


    // In OrderController.java

    // In OrderController.java, add this endpoint:

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @PutMapping("/admin/edit-quantity")
    public ResponseEntity<OrderResponseDTO> editOrderQuantity(
            @RequestBody @Valid EditOrderQuantityDTO editRequest) {

        return ResponseEntity.ok(orderService.editOrderQuantity( editRequest));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/reports/product-branch-matrix")
    public ResponseEntity<Map<String, Object>> getProductBranchOrderMatrix() {
        return ResponseEntity.ok(orderService.getProductBranchOrderMatrix());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
    @GetMapping("/admin/stats/delivered/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProductDeliveryAcrossBranches(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        String methodName = "getProductDeliveryAcrossBranches";
        Map<String, Object> response = new HashMap<>();

        try {
            // Get product info
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

            // Get delivery stats
            List<ProductDeliveredStatsDTO> productStats = orderService.getProductDeliveredStats(year, month, null, productId);

            // Calculate totals
            Double totalQuantity = productStats.stream()
                    .mapToDouble(ProductDeliveredStatsDTO::getTotalQuantityDelivered)
                    .sum();

            // Group by branch
            Map<Long, Map<String, Object>> branchStats = new HashMap<>();
            for (ProductDeliveredStatsDTO stat : productStats) {
                branchStats.computeIfAbsent(stat.getBranchId(), k -> {
                    Map<String, Object> branchInfo = new HashMap<>();
                    branchInfo.put("branchId", stat.getBranchId());
                    branchInfo.put("branchName", stat.getBranchName());
                    branchInfo.put("totalQuantity", 0L);
                    branchInfo.put("monthlyBreakdown", new ArrayList<Map<String, Object>>());
                    return branchInfo;
                });

                Map<String, Object> branchInfo = branchStats.get(stat.getBranchId());
                Long currentTotal = (Long) branchInfo.get("totalQuantity");
                branchInfo.put("totalQuantity", currentTotal + stat.getTotalQuantityDelivered());

                // Add monthly breakdown
                List<Map<String, Object>> monthlyBreakdown = (List<Map<String, Object>>) branchInfo.get("monthlyBreakdown");
                Map<String, Object> monthInfo = new HashMap<>();
                monthInfo.put("year", stat.getYear());
                monthInfo.put("month", stat.getMonth());
                monthInfo.put("monthName", stat.getMonthName());
                monthInfo.put("quantity", stat.getTotalQuantityDelivered());
                monthlyBreakdown.add(monthInfo);
            }

            // Prepare response
            response.put("productId", product.getId());
            response.put("productName", product.getProductName());
            response.put("totalQuantityDelivered", totalQuantity);
            response.put("branchStats", new ArrayList<>(branchStats.values()));

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Product not found: productId={}", productId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting product delivery stats: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to get product delivery statistics");
        }
    }

}