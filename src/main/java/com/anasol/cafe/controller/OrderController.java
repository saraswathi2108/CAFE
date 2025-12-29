package com.anasol.cafe.controller;

import com.anasol.cafe.dto.OrderRequestDTO;
import com.anasol.cafe.dto.OrderResponseDTO;
import com.anasol.cafe.dto.StatusUpdateDTO;
import com.anasol.cafe.entity.OrderStatus;
import com.anasol.cafe.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin("*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    @PostMapping("/order")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody OrderRequestDTO orderRequest) {
        return ResponseEntity.ok(orderService.placeOrder(orderRequest));
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
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

    // Additional endpoint for admin to get user orders
    @PreAuthorize("hasRole('ADMIN')")
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

    // Additional endpoint for admin to get user orders by status
    @PreAuthorize("hasRole('ADMIN')")
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
}