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
import java.util.List;
import java.util.stream.Collectors;

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
                throw new ValidationException(
                        String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                                product.getProductName(), product.getQuantity(), orderRequest.getQuantity())
                );
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

            log.info("Order placed successfully. Order ID: {}", savedOrder.getId());
            return convertToDTO(savedOrder);

        } catch (ResourceNotFoundException e) {
            log.error("Resource not found while placing order: {}", e.getMessage());
            throw e;
        } catch (ValidationException e) {
            log.error("Validation error while placing order: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while placing order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to save order due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while placing order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to place order due to unexpected error", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrders(int page, int size, String sortBy, String direction) {
        try {
            User user = getCurrentAuthenticatedUser();
            log.info("Fetching orders for current user: userId={}, email={}, page={}, size={}",
                    user.getId(), user.getEmail(), page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdWithBranch(user.getId(), pageable);

            return orderPage.map(this::convertToDTO);
        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error fetching my orders: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching my orders", e);
            throw new OrderProcessingException("Failed to fetch orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching my orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch orders due to unexpected error", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrdersByStatus(OrderStatus status, int page, int size, String sortBy, String direction) {
        try {
            if (status == null) {
                throw new ValidationException("Order status cannot be null");
            }

            User user = getCurrentAuthenticatedUser();
            log.info("Fetching {} orders for current user: userId={}, email={}, page={}, size={}",
                    status, user.getId(), user.getEmail(), page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdAndStatusWithBranch(user.getId(), status, pageable);

            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error fetching my {} orders: {}", status, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching my {} orders", status, e);
            throw new OrderProcessingException("Failed to fetch orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching my {} orders: {}", status, e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch orders due to unexpected error", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(int page, int size, String sortBy, String direction) {
        try {
            log.info("Fetching all orders with pagination: page={}, size={}", page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findAllWithUserAndBranch(pageable);

            return orderPage.map(this::convertToDTO);
        } catch (DataAccessException e) {
            log.error("Database error while fetching all orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching all orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch orders due to unexpected error", e);
        }
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus status) {
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

            order.setStatus(status);
            Order updatedOrder = orderRepo.save(order);

            log.info("Order status updated successfully: orderId={}, newStatus={}", orderId, status);
            return convertToDTO(updatedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error updating order status: {}", e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking failure while updating order status: orderId={}", orderId, e);
            throw new OrderProcessingException("Order was modified by another transaction. Please try again.");
        } catch (DataAccessException e) {
            log.error("Database error while updating order status: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to update order status due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while updating order status: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to update order status due to unexpected error", e);
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

                // If cancelling approved order, stock will be restored
                if (newStatus == OrderStatus.CANCELLED) {
                    log.info("Order approved->cancelled: stock will be restored");
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
        try {
            log.info("Fetching pending orders with pagination: page={}, size={}", page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByStatusWithUserAndBranch(OrderStatus.PENDING, pageable);

            return orderPage.map(this::convertToDTO);
        } catch (DataAccessException e) {
            log.error("Database error while fetching pending orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch pending orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching pending orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch pending orders due to unexpected error", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getApprovedOrders(int page, int size, String sortBy, String direction) {
        try {
            log.info("Fetching approved orders with pagination: page={}, size={}", page, size);

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByStatusWithUserAndBranch(OrderStatus.APPROVED, pageable);

            return orderPage.map(this::convertToDTO);
        } catch (DataAccessException e) {
            log.error("Database error while fetching approved orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch approved orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching approved orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch approved orders due to unexpected error", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByUser(Long userId, int page, int size, String sortBy, String direction) {
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

            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error fetching user orders: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching user orders: userId={}", userId, e);
            throw new OrderProcessingException("Failed to fetch user orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching user orders: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch user orders due to unexpected error", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getUserOrdersByStatus(Long userId, OrderStatus status, int page, int size, String sortBy, String direction) {
        try {
            validateUserId(userId);

            log.info("Fetching {} orders for user: userId={}, page={}, size={}", status, userId, page, size);

            // Validate user exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.findByUserIdAndStatusWithBranch(userId, status, pageable);

            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error fetching user {} orders: {}", status, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching user {} orders: userId={}", status, userId, e);
            throw new OrderProcessingException("Failed to fetch user orders due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching user {} orders: {}", status, e.getMessage(), e);
            throw new OrderProcessingException("Failed to fetch user orders due to unexpected error", e);
        }
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }

            log.info("Deleting order: orderId={}", orderId);

            if (!orderRepo.existsById(orderId)) {
                throw new ResourceNotFoundException("Order", "id", orderId);
            }

            orderRepo.deleteById(orderId);
            log.info("Order deleted successfully: orderId={}", orderId);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error deleting order: {}", e.getMessage());
            throw e;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Order not found for deletion: orderId={}", orderId);
            throw new ResourceNotFoundException("Order", "id", orderId);
        } catch (DataAccessException e) {
            log.error("Database error while deleting order: orderId={}", orderId, e);
            throw new OrderProcessingException("Failed to delete order due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to delete order due to unexpected error", e);
        }
    }

    // User can only cancel their own orders
    @Transactional
    public OrderResponseDTO cancelMyOrder(Long orderId) {
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
                throw new ValidationException("You can only cancel your own orders");
            }

            // Validate status transition
            validateStatusTransition(order.getStatus(), OrderStatus.CANCELLED);

            order.setStatus(OrderStatus.CANCELLED);
            Order updatedOrder = orderRepo.save(order);

            log.info("Order cancelled successfully by user: orderId={}", orderId);
            return convertToDTO(updatedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error cancelling order: {}", e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking failure while cancelling order: orderId={}", orderId, e);
            throw new OrderProcessingException("Order was modified by another transaction. Please try again.");
        } catch (DataAccessException e) {
            log.error("Database error while cancelling order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to cancel order due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while cancelling order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to cancel order due to unexpected error", e);
        }
    }

    // User can only receive their own SHIPPED orders
    @Transactional
    public OrderResponseDTO receiveMyOrder(Long orderId) {
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
                throw new ValidationException("You can only receive your own orders");
            }

            // Validate status transition
            validateStatusTransition(order.getStatus(), OrderStatus.DELIVERED);

            order.setStatus(OrderStatus.DELIVERED);
            Order updatedOrder = orderRepo.save(order);

            log.info("Order marked as received by user: orderId={}", orderId);
            return convertToDTO(updatedOrder);

        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Error receiving order: {}", e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking failure while receiving order: orderId={}", orderId, e);
            throw new OrderProcessingException("Order was modified by another transaction. Please try again.");
        } catch (DataAccessException e) {
            log.error("Database error while receiving order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to receive order due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while receiving order: {}", e.getMessage(), e);
            throw new OrderProcessingException("Failed to receive order due to unexpected error", e);
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
}