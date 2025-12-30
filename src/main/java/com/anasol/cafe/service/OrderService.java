package com.anasol.cafe.service;

import com.anasol.cafe.dto.BranchResponse;
import com.anasol.cafe.dto.OrderRequestDTO;
import com.anasol.cafe.dto.OrderResponseDTO;
import com.anasol.cafe.dto.ProductResponse;
import com.anasol.cafe.entity.*;
import com.anasol.cafe.exceptions.OrderProcessingException;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.ValidationException;
import com.anasol.cafe.repository.BranchRepository;
import com.anasol.cafe.repository.OrderRepository;
import com.anasol.cafe.repository.ProductRepo;
import com.anasol.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepo;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProductRepo productRepo;

    // Helper method to get current authenticated user
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("User not authenticated");
        }

        String email = authentication.getName();
        log.debug("Getting authenticated user with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO orderRequest) {
        String methodName = "placeOrder";
        logEntry(methodName, orderRequest);

        try {
            // Validate input
            validateOrderRequest(orderRequest);

            // Get authenticated user FROM TOKEN
            User user = getCurrentAuthenticatedUser();

            // Fetch product with pessimistic lock to prevent concurrent updates
            Product product = productRepo.findByIdWithLock(orderRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", orderRequest.getProductId()));

            // Validate branch exists
            Branch branch = branchRepository.findById(orderRequest.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", orderRequest.getBranchId()));

            // Validate product has sufficient stock
            if (!product.hasSufficientStock(orderRequest.getQuantity())) {
                String errorMsg = String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                        product.getProductName(), product.getQuantity(), orderRequest.getQuantity());
                logValidationError(methodName, errorMsg);
                throw new ValidationException(errorMsg);
            }

            log.info("Placing order: userId={}, email={}, branchId={}, productId={}, quantity={}, currentStock={}",
                    user.getId(), user.getEmail(), orderRequest.getBranchId(),
                    orderRequest.getProductId(), orderRequest.getQuantity(), product.getQuantity());

            // Reduce product stock
            product.reduceStock(orderRequest.getQuantity());
            productRepo.save(product); // Save updated product quantity

            log.info("Product stock reduced: productId={}, newQuantity={}",
                    product.getId(), product.getQuantity());

            Order order = new Order();
            order.setProduct(product);
            order.setUser(user);
            order.setBranchId(orderRequest.getBranchId());
            order.setQuantity(orderRequest.getQuantity());
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());

            Order savedOrder = orderRepo.save(order);

            // Reload the order with all relationships
            savedOrder = orderRepo.findByIdWithAllRelations(savedOrder.getId())
                    .orElse(savedOrder);

            logSuccess(methodName, "Order placed successfully. Order ID: " + savedOrder.getId());
            return convertToDTO(savedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to save order due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to place order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrders(int page, int size, String sortBy, String direction) {
        String methodName = "getMyOrders";
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("size", size);
        params.put("sortBy", sortBy);
        params.put("direction", direction);
        logEntry(methodName, params);

        try {
            User user = getCurrentAuthenticatedUser();
            log.info("Fetching orders for current user: userId={}, email={}, page={}, size={}",
                    user.getId(), user.getEmail(), page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdWithBranch(user.getId(), pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " orders");
            return orderPage.map(this::convertToDTO);
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrdersByStatus(OrderStatus status, int page, int size, String sortBy, String direction) {
        String methodName = "getMyOrdersByStatus";
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            if (status == null) {
                throw new ValidationException("Order status cannot be null");
            }

            User user = getCurrentAuthenticatedUser();
            log.info("Fetching {} orders for current user: userId={}, email={}, page={}, size={}",
                    status, user.getId(), user.getEmail(), page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdAndStatusWithBranch(user.getId(), status, pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " " + status + " orders");
            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch " + status + " orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch " + status + " orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(int page, int size, String sortBy, String direction) {
        String methodName = "getAllOrders";
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            log.info("Fetching all orders with pagination: page={}, size={}", page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findAllWithUserAndBranch(pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getTotalElements() + " total orders");
            return orderPage.map(this::convertToDTO);
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch all orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch all orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus status) {
        String methodName = "updateOrderStatus";
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("status", status);
        logEntry(methodName, params);

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }
            if (status == null) {
                throw new ValidationException("Order status cannot be null");
            }

            log.info("Updating order status: orderId={}, newStatus={}", orderId, status);

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Validate status transition
            validateStatusTransition(order.getStatus(), status);

            // If order is being rejected, restore stock
            if (order.getStatus() == OrderStatus.PENDING && status == OrderStatus.REJECTED) {
                restoreProductStock(order);
            }

            // If order is being cancelled from APPROVED status, restore stock
            if (order.getStatus() == OrderStatus.APPROVED && status == OrderStatus.CANCELLED) {
                restoreProductStock(order);
            }

            order.setStatus(status);
            Order updatedOrder = orderRepo.save(order);

            logSuccess(methodName, "Order status updated successfully: orderId=" + orderId + ", newStatus=" + status);
            return convertToDTO(updatedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            String errorMsg = "Order was modified by another transaction. Please try again.";
            logOptimisticLockError(methodName, orderId, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (DataAccessException e) {
            String errorMsg = "Failed to update order status due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to update order status due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        log.debug("Validating status transition from {} to {}", currentStatus, newStatus);

        // If status is not changing, allow it
        if (currentStatus == newStatus) {
            return;
        }

        // Define allowed transitions
        switch (currentStatus) {
            case PENDING:
                // From PENDING, can go to: APPROVED, REJECTED, CANCELLED
                if (newStatus != OrderStatus.APPROVED &&
                        newStatus != OrderStatus.REJECTED &&
                        newStatus != OrderStatus.CANCELLED) {
                    throw new ValidationException(
                            String.format("Cannot transition order from %s to %s. " +
                                            "Allowed transitions: APPROVED, REJECTED, or CANCELLED.",
                                    currentStatus, newStatus)
                    );
                }
                break;

            case APPROVED:
                // From APPROVED, can go to: SHIPPED, CANCELLED
                if (newStatus != OrderStatus.SHIPPED &&
                        newStatus != OrderStatus.CANCELLED) {
                    throw new ValidationException(
                            String.format("Cannot transition order from %s to %s. " +
                                            "Allowed transitions: SHIPPED or CANCELLED.",
                                    currentStatus, newStatus)
                    );
                }
                break;

            case SHIPPED:
                // From SHIPPED, can go to: DELIVERED
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new ValidationException(
                            String.format("Cannot transition order from %s to %s. " +
                                            "Allowed transition: DELIVERED.",
                                    currentStatus, newStatus)
                    );
                }
                break;

            case DELIVERED:
                // From DELIVERED, cannot change status
                throw new ValidationException(
                        String.format("Cannot change status of a %s order.", currentStatus)
                );

            case REJECTED:
                // From REJECTED, cannot change status
                throw new ValidationException(
                        String.format("Cannot change status of a %s order.", currentStatus)
                );

            case CANCELLED:
                // From CANCELLED, cannot change status
                throw new ValidationException(
                        String.format("Cannot change status of a %s order.", currentStatus)
                );

            default:
                throw new ValidationException(
                        String.format("Unknown current status: %s", currentStatus)
                );
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getPendingOrders(int page, int size, String sortBy, String direction) {
        return getOrdersByStatus(OrderStatus.PENDING, page, size, sortBy, direction);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getApprovedOrders(int page, int size, String sortBy, String direction) {
        return getOrdersByStatus(OrderStatus.APPROVED, page, size, sortBy, direction);
    }

    // NEW: Get rejected orders
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getRejectedOrders(int page, int size, String sortBy, String direction) {
        return getOrdersByStatus(OrderStatus.REJECTED, page, size, sortBy, direction);
    }

    // NEW: Get cancelled orders
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getCancelledOrders(int page, int size, String sortBy, String direction) {
        return getOrdersByStatus(OrderStatus.CANCELLED, page, size, sortBy, direction);
    }

    // NEW: Get shipped orders
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getShippedOrders(int page, int size, String sortBy, String direction) {
        return getOrdersByStatus(OrderStatus.SHIPPED, page, size, sortBy, direction);
    }

    // NEW: Get delivered orders
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getDeliveredOrders(int page, int size, String sortBy, String direction) {
        return getOrdersByStatus(OrderStatus.DELIVERED, page, size, sortBy, direction);
    }

    private Page<OrderResponseDTO> getOrdersByStatus(OrderStatus status, int page, int size, String sortBy, String direction) {
        String methodName = "getOrdersByStatus";
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            log.info("Fetching {} orders with pagination: page={}, size={}", status, page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByStatusWithUserAndBranch(status, pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " " + status + " orders");
            return orderPage.map(this::convertToDTO);
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch " + status + " orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch " + status + " orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByUser(Long userId, int page, int size, String sortBy, String direction) {
        String methodName = "getOrdersByUser";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            if (userId == null) {
                throw new ValidationException("User ID cannot be null");
            }

            log.info("Fetching orders for user: userId={}, page={}, size={}", userId, page, size);

            // Validate user exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdWithBranch(userId, pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " orders for user " + userId);
            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch user orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch user orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getUserOrdersByStatus(Long userId, OrderStatus status, int page, int size, String sortBy, String direction) {
        String methodName = "getUserOrdersByStatus";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("status", status);
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            validateUserId(userId);

            log.info("Fetching {} orders for user: userId={}, page={}, size={}", status, userId, page, size);

            // Validate user exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdAndStatusWithBranch(userId, status, pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " " + status + " orders for user " + userId);
            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch user " + status + " orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch user " + status + " orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        String methodName = "deleteOrder";
        logEntry(methodName, orderId);

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }

            log.info("Deleting order: orderId={}", orderId);

            if (!orderRepo.existsById(orderId)) {
                throw new ResourceNotFoundException("Order", "id", orderId);
            }

            orderRepo.deleteById(orderId);
            logSuccess(methodName, "Order deleted successfully: orderId=" + orderId);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Order not found for deletion: orderId={}", orderId);
            throw new ResourceNotFoundException("Order", "id", orderId);
        } catch (DataAccessException e) {
            String errorMsg = "Failed to delete order due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to delete order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    // User can only cancel their own orders
    @Transactional
    public OrderResponseDTO cancelMyOrder(Long orderId) {
        String methodName = "cancelMyOrder";
        logEntry(methodName, orderId);

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }

            User currentUser = getCurrentAuthenticatedUser();
            log.info("User cancelling order: orderId={}, userId={}, email={}",
                    orderId, currentUser.getId(), currentUser.getEmail());

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Verify the order belongs to the current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                String errorMsg = "You can only cancel your own orders";
                logValidationError(methodName, errorMsg);
                throw new ValidationException(errorMsg);
            }

            // Validate status transition
            validateStatusTransition(order.getStatus(), OrderStatus.CANCELLED);

            // If order is APPROVED and being cancelled, restore stock
            if (order.getStatus() == OrderStatus.APPROVED) {
                restoreProductStock(order);
            }

            order.setStatus(OrderStatus.CANCELLED);
            Order updatedOrder = orderRepo.save(order);

            logSuccess(methodName, "Order cancelled successfully by user: orderId=" + orderId);
            return convertToDTO(updatedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            String errorMsg = "Order was modified by another transaction. Please try again.";
            logOptimisticLockError(methodName, orderId, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (DataAccessException e) {
            String errorMsg = "Failed to cancel order due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to cancel order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    // User can only receive their own SHIPPED orders
    @Transactional
    public OrderResponseDTO receiveMyOrder(Long orderId) {
        String methodName = "receiveMyOrder";
        logEntry(methodName, orderId);

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }

            User currentUser = getCurrentAuthenticatedUser();
            log.info("User receiving order: orderId={}, userId={}, email={}",
                    orderId, currentUser.getId(), currentUser.getEmail());

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

            // Verify the order belongs to the current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                String errorMsg = "You can only receive your own orders";
                logValidationError(methodName, errorMsg);
                throw new ValidationException(errorMsg);
            }

            // Validate status transition
            validateStatusTransition(order.getStatus(), OrderStatus.DELIVERED);

            order.setStatus(OrderStatus.DELIVERED);
            Order updatedOrder = orderRepo.save(order);

            logSuccess(methodName, "Order marked as received by user: orderId=" + orderId);
            return convertToDTO(updatedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            String errorMsg = "Order was modified by another transaction. Please try again.";
            logOptimisticLockError(methodName, orderId, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (DataAccessException e) {
            String errorMsg = "Failed to receive order due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to receive order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    private void validateOrderRequest(OrderRequestDTO orderRequest) {
        if (orderRequest == null) {
            throw new ValidationException("Order request cannot be null");
        }

        if (orderRequest.getBranchId() == null) {
            throw new ValidationException("Branch ID cannot be null");
        }

        if (orderRequest.getProductId() == null) {
            throw new ValidationException("Product ID cannot be null");
        }

        if (orderRequest.getQuantity() == null || orderRequest.getQuantity() <= 0) {
            throw new ValidationException("Quantity must be greater than zero");
        }
    }

    private OrderResponseDTO convertToDTO(Order order) {
        try {
            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setId(order.getId());

            if (order.getProduct() != null) {
                dto.setProductId(order.getProduct().getId());
            }

            dto.setBranchId(order.getBranchId());
            dto.setQuantity(order.getQuantity());
            dto.setStatus(order.getStatus());
            dto.setCreatedAt(order.getCreatedAt());

            if (order.getBranch() != null) {
                dto.setBranchResponse(convertBranchToDTO(order.getBranch()));
            }

            if (order.getProduct() != null) {
                dto.setProductResponse(convertProductToDTO(order.getProduct()));
            }

            return dto;
        } catch (Exception e) {
            log.error("Error converting order to DTO: orderId={}", order.getId(), e);
            throw new OrderProcessingException("Failed to process order data", e);
        }
    }

    private BranchResponse convertBranchToDTO(Branch branch) {
        BranchResponse dto = new BranchResponse();
        dto.setId(branch.getId());
        dto.setBranchCode(branch.getBranchCode());
        dto.setBranchName(branch.getBranchName());
        dto.setActive(branch.isActive());
        return dto;
    }

    private ProductResponse convertProductToDTO(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        return dto;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        // Set default values if not provided
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (sortBy == null || sortBy.isEmpty()) sortBy = "createdAt";
        if (direction == null || direction.isEmpty()) direction = "desc";

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    // NEW: Restore product stock when order is rejected or cancelled
    private void restoreProductStock(Order order) {
        if (order.getProduct() != null) {
            Product product = order.getProduct();
            Long restoredQuantity = order.getQuantity();
            product.increaseStock(restoredQuantity);
            productRepo.save(product);
            log.info("Product stock restored: productId={}, restoredQuantity={}, newQuantity={}",
                    product.getId(), restoredQuantity, product.getQuantity());
        }
    }

    // Enhanced logging methods for better exception tracking
    private void logEntry(String methodName, Object params) {
        log.debug("Entering {} with params: {}", methodName, params);
    }

    private void logSuccess(String methodName, String message) {
        log.info("{} - Success: {}", methodName, message);
    }

    private void logValidationError(String methodName, String error) {
        log.warn("{} - Validation Error: {}", methodName, error);
    }

    private void logDatabaseError(String methodName, String error, Exception e) {
        log.error("{} - Database Error: {}", methodName, error, e);
    }

    private void logUnexpectedError(String methodName, String error, Exception e) {
        log.error("{} - Unexpected Error: {}", methodName, error, e);
    }

    private void logOptimisticLockError(String methodName, Long orderId, Exception e) {
        log.error("{} - Optimistic Lock Error for orderId={}: {}", methodName, orderId, e.getMessage());
    }
}