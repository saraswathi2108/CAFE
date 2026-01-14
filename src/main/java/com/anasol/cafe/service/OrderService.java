package com.anasol.cafe.service;

import com.anasol.cafe.dto.*;
import com.anasol.cafe.entity.*;
import com.anasol.cafe.exceptions.OrderProcessingException;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.ValidationException;
import com.anasol.cafe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepo;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProductRepo productRepo;
    private final CartRepository cartRepository;
    private final CartItemsRepository cartItemsRepository;
    private final S3Service s3Service;

    private final NotificationService notificationService;

    // Helper method to get current authenticated user
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("User not authenticated");
        }

        String email = authentication.getName();
        log.debug("Getting authenticated user with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not Found with email: "+ email));
    }

    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO orderRequest) {
        String methodName = "placeOrder";
        logEntry(methodName, orderRequest);

        Order savedOrder = null;
        User user = null;
        Product product = null;
        Branch branch = null;

        try {
            // Validate input
            validateOrderRequest(orderRequest);

            // Get authenticated user FROM TOKEN
            user = getCurrentAuthenticatedUser();

            // Fetch product with pessimistic lock to prevent concurrent updates
            product = productRepo.findByIdWithLock(orderRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Not Found with Id: "+ orderRequest.getProductId()));

            // Validate branch exists
            branch = branchRepository.findById(orderRequest.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch Not Found with id: "+ orderRequest.getBranchId()));

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

            // Create order with OrderItems
            Order order = new Order();
            order.setUser(user);
            order.setBranchId(orderRequest.getBranchId());
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());

            // Create and add OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(orderRequest.getQuantity());
            orderItem.setOrder(order);

            // Initialize order items list and add item
            order.setOrderItems(new ArrayList<>());
            order.getOrderItems().add(orderItem);

            savedOrder = orderRepo.save(order);

            // Reload the order with all relationships
            savedOrder = orderRepo.findByIdWithOrderItems(savedOrder.getId())
                    .orElse(savedOrder);

            logSuccess(methodName, "Order placed successfully. Order ID: " + savedOrder.getId());

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

        // Send notifications AFTER transaction completes
        if (savedOrder != null && user != null && product != null && branch != null) {
            try {
                sendOrderPlacedNotification(user, savedOrder, product, branch);
                sendNewOrderNotificationToAdmin(savedOrder, product, branch, user);
            } catch (Exception e) {
                log.error("Failed to send notifications for order #{}: {}", savedOrder.getId(), e.getMessage());
                // Don't throw exception - notifications are secondary
            }
        }

        return convertToDTO(savedOrder);
    }


    @Transactional
    public OrderResponseDTO placeOrderFromCart(CartOrderRequestDTO cartOrderRequest) {
        String methodName = "placeOrderFromCart";
        logEntry(methodName, cartOrderRequest);

        Order savedOrder = null;
        User user = null;
        Branch branch = null;

        try {
            // Validate input
            validateCartOrderRequest(cartOrderRequest);

            // Get authenticated user
            user = getCurrentAuthenticatedUser();

            // Validate branch exists
            branch = branchRepository.findById(cartOrderRequest.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch Not Found with id: "+ cartOrderRequest.getBranchId()));

            // Get cart with items
            Cart cart = cartRepository.findById(cartOrderRequest.getCartId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cart Not Found with id: "+ cartOrderRequest.getCartId()));

            // Verify cart belongs to current user
            if (!cart.getUser().getId().equals(user.getId())) {
                throw new ValidationException("Cart does not belong to current user");
            }

            // Get all cart items
            List<CartItems> cartItems = cartItemsRepository.findByCartId(cartOrderRequest.getCartId());

            if (cartItems.isEmpty()) {
                throw new ValidationException("Cart is empty");
            }

            log.info("Creating single order from cart: userId={}, cartId={}, branchId={}, itemsCount={}",
                    user.getId(), cartOrderRequest.getCartId(), cartOrderRequest.getBranchId(), cartItems.size());

            // For cart orders, we need to get the first product to satisfy NOT NULL constraint
            CartItems firstCartItem = cartItems.get(0);
            Product firstProduct = productRepo.findById(firstCartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Not Found with Id: "+ firstCartItem.getProduct().getId()));

            // Create a SINGLE order
            Order order = new Order();
            order.setUser(user);
            order.setBranchId(cartOrderRequest.getBranchId());
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setOrderItems(new ArrayList<>());

            // Process each cart item
            for (CartItems cartItem : cartItems) {
                Product product = productRepo.findById(cartItem.getProduct().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product Not Found with Id: "+ cartItem.getProduct().getId()));

                // Validate stock
                if (!product.hasSufficientStock(cartItem.getQuantity())) {
                    String errorMsg = String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                            product.getProductName(), product.getQuantity(), cartItem.getQuantity());
                    throw new ValidationException(errorMsg);
                }

                // Reduce product stock
                product.reduceStock(cartItem.getQuantity());
                productRepo.save(product);

                // Create order item and link to the SAME order
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setOrder(order);

                // Add to order's item list
                order.getOrderItems().add(orderItem);

                log.info("Added order item: productId={}, quantity={}",
                        product.getId(), cartItem.getQuantity());
            }

            // Save the SINGLE order with all items
            savedOrder = orderRepo.save(order);

            // Clear cart after successful order
            cartItemsRepository.deleteAll(cartItems);

            // Load the order with relationships
            savedOrder = orderRepo.findByIdWithOrderItems(savedOrder.getId())
                    .orElse(savedOrder);

            logSuccess(methodName, "Cart converted to single order successfully. Order ID: " + savedOrder.getId() +
                    ", Items count: " + savedOrder.getOrderItems().size());

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to process cart order due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to process cart order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }

        // Send notifications AFTER transaction completes
        if (savedOrder != null && user != null && branch != null) {
            try {
                sendCartOrderPlacedNotification(user, savedOrder, branch);
                sendNewCartOrderNotificationToAdmin(savedOrder, branch, user);
            } catch (Exception e) {
                log.error("Failed to send notifications for cart order #{}: {}", savedOrder.getId(), e.getMessage());
                // Don't throw exception - notifications are secondary
            }
        }

        return convertToDTO(savedOrder);
    }
    // NEW: Get order with all items
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderWithItems(Long orderId) {
        String methodName = "getOrderWithItems";
        logEntry(methodName, orderId);

        try {
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order Not Found with Id: "+ orderId));

            return convertToDTO(order);
        } catch (ResourceNotFoundException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch order details due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch order details due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }



    // Keep all your existing methods unchanged...
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

        Order updatedOrder = null;
        OrderStatus oldStatus = null;
        User orderUser = null;

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }
            if (status == null) {
                throw new ValidationException("Order status cannot be null");
            }

            log.info("Updating order status: orderId={}, newStatus={}", orderId, status);

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order Not Found with Id: "+orderId));

            // Validate status transition
            validateStatusTransition(order.getStatus(), status);

            oldStatus = order.getStatus();
            orderUser = order.getUser();

            // If order is being rejected, restore stock
            if (order.getStatus() == OrderStatus.PENDING && status == OrderStatus.REJECTED) {
                restoreProductStock(order);
            }

            // If order is being cancelled from APPROVED status, restore stock
            if (order.getStatus() == OrderStatus.APPROVED && status == OrderStatus.CANCELLED) {
                restoreProductStock(order);
            }

            order.setStatus(status);
            updatedOrder = orderRepo.save(order);

            logSuccess(methodName, "Order status updated successfully: orderId=" + orderId + ", newStatus=" + status);

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
            throw new OrderProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to update order status due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg);
        }

        // Send notification AFTER transaction completes
        if (updatedOrder != null && oldStatus != null && orderUser != null) {
            try {
                sendOrderStatusUpdateNotification(updatedOrder, oldStatus, status);
            } catch (Exception e) {
                log.error("Failed to send status update notification for order #{}: {}", orderId, e.getMessage());
                // Don't throw exception - notifications are secondary
            }
        }

        return convertToDTO(updatedOrder);
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
                    .orElseThrow(() -> new ResourceNotFoundException("User Not Found with Id: "+ userId));

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
                    .orElseThrow(() -> new ResourceNotFoundException("User Not Found with Id: "+ userId));

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
                throw new ResourceNotFoundException("Order Not Found with Id: "+ orderId);
            }

            orderRepo.deleteById(orderId);
            logSuccess(methodName, "Order deleted successfully: orderId=" + orderId);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Order not found for deletion: orderId={}", orderId);
            throw new ResourceNotFoundException("Order Not Found for deletion: "+ orderId);
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

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
    }



    @Transactional
    public OrderResponseDTO cancelMyOrder(Long orderId) {
        String methodName = "cancelMyOrder";
        logEntry(methodName, orderId);

        Order updatedOrder = null;
        OrderStatus oldStatus = null;
        User orderUser = null;

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }

            User currentUser = getCurrentAuthenticatedUser();
            log.info("User cancelling order: orderId={}, userId={}, email={}",
                    orderId, currentUser.getId(), currentUser.getEmail());

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order Not Found with Id: "+ orderId));

            // Verify the order belongs to the current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                String errorMsg = "You can only cancel your own orders";
                logValidationError(methodName, errorMsg);
                throw new ValidationException(errorMsg);
            }

            // Validate status transition
            validateStatusTransition(order.getStatus(), OrderStatus.CANCELLED);

            oldStatus = order.getStatus();
            orderUser = order.getUser();

            // If order is APPROVED and being cancelled, restore stock
            if (order.getStatus() == OrderStatus.APPROVED) {
                restoreProductStock(order);
            }

            order.setStatus(OrderStatus.CANCELLED);
            updatedOrder = orderRepo.save(order);

            logSuccess(methodName, "Order cancelled successfully by user: orderId=" + orderId);

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
            throw new OrderProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to cancel order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg);
        }

        // Send notification AFTER transaction completes
        if (updatedOrder != null && oldStatus != null && orderUser != null) {
            try {
                sendOrderCancelledNotification(updatedOrder, oldStatus);
            } catch (Exception e) {
                log.error("Failed to send cancellation notification for order #{}: {}", orderId, e.getMessage());
                // Don't throw exception - notifications are secondary
            }
        }

        return convertToDTO(updatedOrder);
    }

    // In OrderService.java, add this method:
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> searchOrders(
            Long orderId,
            String branchName,
            LocalDate date,
            int page,
            int size,
            String sortBy,
            String direction) {

        String methodName = "searchOrders";
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("branchName", branchName);
        params.put("date", date);
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            User user = getCurrentAuthenticatedUser();
            Pageable pageable = createPageable(page, size, sortBy, direction);

            Page<Order> orderPage;

            // If user is admin, search all orders
            if (user.getRole().name().equals("ADMIN")) {
                orderPage = orderRepo.searchAllOrders(orderId, branchName, date, pageable);
            }
            // If user is manager/staff, search only their orders
            else {
                // You might want to create a separate method for user-specific search
                // For now, using findAll and filtering in memory (not optimal for large datasets)
                // Alternatively, create another repository method
                orderPage = orderRepo.findByUserIdWithBranch(user.getId(), pageable);

                // Filter results in memory (simplified approach)
                List<Order> filteredOrders = orderPage.getContent().stream()
                        .filter(order -> {
                            boolean matches = true;
                            if (orderId != null) {
                                matches = matches && order.getId().equals(orderId);
                            }
                            if (branchName != null && !branchName.isEmpty()) {
                                matches = matches && order.getBranch() != null &&
                                        order.getBranch().getBranchName().toLowerCase()
                                                .contains(branchName.toLowerCase());
                            }
                            if (date != null) {
                                matches = matches && order.getCreatedAt().toLocalDate().equals(date);
                            }
                            return matches;
                        })
                        .collect(Collectors.toList());

                // Create a new Page with filtered results
                orderPage = new PageImpl<>(filteredOrders, pageable, filteredOrders.size());
            }

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " orders from search");
            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to search orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to search orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    // For admin-specific search
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> searchAllOrders(
            Long orderId,
            String branchName,
            LocalDate date,
            int page,
            int size,
            String sortBy,
            String direction) {

        String methodName = "searchAllOrders";
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("branchName", branchName);
        params.put("date", date);
        params.put("page", page);
        params.put("size", size);
        logEntry(methodName, params);

        try {
            Pageable pageable = createPageable(page, size, sortBy, direction);
            Page<Order> orderPage = orderRepo.searchAllOrders(orderId, branchName, date, pageable);

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() + " orders from admin search");
            return orderPage.map(this::convertToDTO);

        } catch (DataAccessException e) {
            String errorMsg = "Failed to search all orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to search all orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional
    public OrderResponseDTO receiveMyOrder(Long orderId) {
        String methodName = "receiveMyOrder";
        logEntry(methodName, orderId);

        Order updatedOrder = null;
        OrderStatus oldStatus = null;
        User orderUser = null;

        try {
            if (orderId == null) {
                throw new ValidationException("Order ID cannot be null");
            }

            User currentUser = getCurrentAuthenticatedUser();
            log.info("User receiving order: orderId={}, userId={}, email={}",
                    orderId, currentUser.getId(), currentUser.getEmail());

            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order Id Not Found: "+ orderId));

            // Verify the order belongs to the current user
            if (!order.getUser().getId().equals(currentUser.getId())) {
                String errorMsg = "You can only receive your own orders";
                logValidationError(methodName, errorMsg);
                throw new ValidationException(errorMsg);
            }

            // Validate status transition
            validateStatusTransition(order.getStatus(), OrderStatus.DELIVERED);

            oldStatus = order.getStatus();
            orderUser = order.getUser();

            order.setStatus(OrderStatus.DELIVERED);
            updatedOrder = orderRepo.save(order);

            logSuccess(methodName, "Order marked as received by user: orderId=" + orderId);

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
            throw new OrderProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to receive order due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg);
        }

        // Send notification AFTER transaction completes
        if (updatedOrder != null && oldStatus != null && orderUser != null) {
            try {
                sendOrderDeliveredNotification(updatedOrder, oldStatus);
            } catch (Exception e) {
                log.error("Failed to send delivery notification for order #{}: {}", orderId, e.getMessage());
                // Don't throw exception - notifications are secondary
            }
        }

        return convertToDTO(updatedOrder);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendOrderPlacedNotification(User user, Order order, Product product, Branch branch) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            String orderDate = order.getCreatedAt().format(formatter);

            String message = String.format(
                    "Your order #%d has been placed successfully. " +
                            "Product: %s, Quantity: %d, Branch: %s. Order Date: %s",
                    order.getId(),
                    product.getProductName(),
                    getTotalOrderQuantity(order),
                    branch.getBranchName(),
                    orderDate
            );

            // Use the direct notification method that takes all parameters
            notificationService.sendNotificationToUser(
                    user.getEmail(),
                    message,
                    "System",
                    "ORDER_PLACED",
                    "/orders/" + order.getId(),
                    "ORDERS",
                    "SUCCESS",
                    "Order Placed Successfully - #" + order.getId()
            );

            log.info("Order placed notification sent to user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order placed notification: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
        }
    }

    /**
     * Send notification to admin about new order
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendNewOrderNotificationToAdmin(Order order, Product product, Branch branch, User user) {
        try {
            // Find admin users
            List<User> adminUsers = userRepository.findByRole(Role.ADMIN);

            if (adminUsers.isEmpty()) {
                log.warn("No admin users found to send notification");
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            String orderDate = order.getCreatedAt().format(formatter);

            String message = String.format(
                    "New order #%d received from %s. " +
                            "Product: %s, Quantity: %d, Branch: %s. Order Date: %s",
                    order.getId(),
                    user.getEmail(),
                    product.getProductName(),
                    getTotalOrderQuantity(order),
                    branch.getBranchName(),
                    orderDate
            );

            // Send notification to each admin
            for (User admin : adminUsers) {
                notificationService.sendNotificationToUser(
                        admin.getEmail(),
                        message,
                        "System",
                        "NEW_ORDER",
                        "/admin/orders/" + order.getId(),
                        "ORDERS",
                        "ALERT",
                        "New Order Received - #" + order.getId()
                );
            }

            log.info("New order notification sent to {} admin users", adminUsers.size());
        } catch (Exception e) {
            log.error("Failed to send new order notification to admin: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
        }
    }

    /**
     * Send notification to user when order status is updated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendOrderStatusUpdateNotification(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            String statusMessage = getStatusUpdateMessage(newStatus);

            String message = String.format(
                    "Your order #%d status has been updated from %s to %s. %s",
                    order.getId(),
                    oldStatus,
                    newStatus,
                    statusMessage
            );

            notificationService.sendNotificationToUser(
                    order.getUser().getEmail(),
                    message,
                    "System",
                    "ORDER_STATUS_UPDATE",
                    "/orders/" + order.getId(),
                    "ORDERS",
                    "INFO",
                    "Order Status Updated - #" + order.getId()
            );

            log.info("Order status update notification sent for order #{}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order status update notification: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
        }
    }

    /**
     * Send notification to user when order is cancelled
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendOrderCancelledNotification(Order order, OrderStatus oldStatus) {
        try {
            String message = String.format(
                    "Your order #%d has been cancelled. " +
                            "Previous status: %s. " +
                            "If you have any questions, please contact support.",
                    order.getId(),
                    oldStatus
            );

            notificationService.sendNotificationToUser(
                    order.getUser().getEmail(),
                    message,
                    "System",
                    "ORDER_CANCELLED",
                    "/orders/" + order.getId(),
                    "ORDERS",
                    "WARNING",
                    "Order Cancelled - #" + order.getId()
            );

            log.info("Order cancelled notification sent for order #{}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order cancelled notification: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
        }
    }

    /**
     * Send notification to user when order is delivered
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendOrderDeliveredNotification(Order order, OrderStatus oldStatus) {
        try {
            String message = String.format(
                    "Your order #%d has been delivered successfully! " +
                            "Thank you for shopping with us. " +
                            "We hope you enjoy your purchase.",
                    order.getId()
            );

            notificationService.sendNotificationToUser(
                    order.getUser().getEmail(),
                    message,
                    "System",
                    "ORDER_DELIVERED",
                    "/orders/" + order.getId(),
                    "ORDERS",
                    "SUCCESS",
                    "Order Delivered - #" + order.getId()
            );

            log.info("Order delivered notification sent for order #{}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send order delivered notification: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
        }
    }

    /**
     * Send notification to user when cart order is placed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendCartOrderPlacedNotification(User user, Order order, Branch branch) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            String orderDate = order.getCreatedAt().format(formatter);
            int itemCount = order.getOrderItems() != null ? order.getOrderItems().size() : 0;

            String message = String.format(
                    "Your cart order #%d has been placed successfully. " +
                            "Items: %d, Total Quantity: %d, Branch: %s. Order Date: %s",
                    order.getId(),
                    itemCount,
                    getTotalOrderQuantity(order),
                    branch.getBranchName(),
                    orderDate
            );

            notificationService.sendNotificationToUser(
                    user.getEmail(),
                    message,
                    "System",
                    "CART_ORDER_PLACED",
                    "/orders/" + order.getId(),
                    "ORDERS",
                    "SUCCESS",
                    "Cart Order Placed - #" + order.getId()
            );

            log.info("Cart order placed notification sent to user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send cart order placed notification: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
        }
    }

    /**
     * Send notification to admin about new cart order
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendNewCartOrderNotificationToAdmin(Order order, Branch branch, User user) {
        try {
            // Find admin users
            List<User> adminUsers = userRepository.findByRole(Role.ADMIN);

            if (adminUsers.isEmpty()) {
                log.warn("No admin users found to send notification");
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            String orderDate = order.getCreatedAt().format(formatter);
            int itemCount = order.getOrderItems() != null ? order.getOrderItems().size() : 0;

            String message = String.format(
                    "New cart order #%d received from %s. " +
                            "Items: %d, Total Quantity: %d, Branch: %s. Order Date: %s",
                    order.getId(),
                    user.getEmail(),
                    itemCount,
                    getTotalOrderQuantity(order),
                    branch.getBranchName(),
                    orderDate
            );

            // Send notification to each admin
            for (User admin : adminUsers) {
                notificationService.sendNotificationToUser(
                        admin.getEmail(),
                        message,
                        "System",
                        "NEW_CART_ORDER",
                        "/admin/orders/" + order.getId(),
                        "ORDERS",
                        "ALERT",
                        "New Cart Order - #" + order.getId()
                );
            }

            log.info("New cart order notification sent to {} admin users", adminUsers.size());
        } catch (Exception e) {
            log.error("Failed to send new cart order notification to admin: {}", e.getMessage());
            throw e; // Re-throw to mark transaction for rollback if needed
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

    private Long getTotalOrderQuantity(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return 0L;
        }
        return order.getOrderItems().stream()
                .mapToLong(OrderItem::getQuantity)
                .sum();
    }

    /**
     * Helper method to get status update message
     */
    private String getStatusUpdateMessage(OrderStatus status) {
        switch (status) {
            case APPROVED:
                return "Your order has been approved and is being processed.";
            case SHIPPED:
                return "Your order has been shipped and is on its way.";
            case DELIVERED:
                return "Your order has been delivered. Please confirm receipt.";
            case REJECTED:
                return "Your order has been rejected. Please contact support for more information.";
            case CANCELLED:
                return "Your order has been cancelled.";
            default:
                return "";
        }
    }

    // Keep all other existing methods (getMyOrders, getAllOrders, searchOrders, etc.)
    // ... (all other methods remain exactly the same as in your original code)

    private void validateCartOrderRequest(CartOrderRequestDTO request) {
        if (request == null) {
            throw new ValidationException("Cart order request cannot be null");
        }
        if (request.getCartId() == null) {
            throw new ValidationException("Cart ID cannot be null");
        }
        if (request.getBranchId() == null) {
            throw new ValidationException("Branch ID cannot be null");
        }
    }


    // In OrderService.java, add these methods:

    @Transactional(readOnly = true)
    public List<DeliveredOrderStatsDTO> getDeliveredOrdersStats(Integer year, Integer month, Long branchId) {
        String methodName = "getDeliveredOrdersStats";
        logEntry(methodName, Map.of("year", year, "month", month, "branchId", branchId));

        try {
            List<DeliveredOrderStatsDTO> stats = orderRepo.getDeliveredOrdersStats(year, month, branchId);
            logSuccess(methodName, "Retrieved delivered orders stats: " + stats.size() + " records");
            return stats;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch delivered orders statistics due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch delivered orders statistics due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductDeliveredStatsDTO> getProductDeliveredStats(
            Integer year, Integer month, Long branchId, Long productId) {

        String methodName = "getProductDeliveredStats";

        // Create a safe map without null values
        Map<String, Object> params = new HashMap<>();
        if (year != null) params.put("year", year);
        if (month != null) params.put("month", month);
        if (branchId != null) params.put("branchId", branchId);
        if (productId != null) params.put("productId", productId);

        logEntry(methodName, params);

        try {
            // Use native query approach instead
            List<Object[]> results = orderRepo.getProductDeliveredStatsNative(year, month, branchId, productId);

            // Map results to DTO
            List<ProductDeliveredStatsDTO> stats = results.stream()
                    .map(this::mapToProductDeliveredStatsDTO)
                    .collect(Collectors.toList());

            logSuccess(methodName, "Retrieved product delivered stats: " + stats.size() + " records");
            return stats;

        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch product delivered statistics due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch product delivered statistics due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }

    }
    private ProductDeliveredStatsDTO mapToProductDeliveredStatsDTO(Object[] row) {
        return new ProductDeliveredStatsDTO(
                ((Number) row[0]).longValue(),    // productId
                (String) row[1],                  // productName
                (String) row[2],                  // categoryName
                ((Number) row[3]).longValue(),    // branchId
                (String) row[4],                  // branchName
                row[5] != null ? ((Number) row[5]).intValue() : null, // year
                row[6] != null ? ((Number) row[6]).intValue() : null, // month
                (String) row[7],                  // monthName
                row[8] != null ? ((Number) row[8]).longValue() : 0L   // totalQuantityDelivered
        );
    }


    @Transactional(readOnly = true)
    public List<BranchDeliveredSummaryDTO> getBranchDeliveredSummary(Integer year) {
        String methodName = "getBranchDeliveredSummary";
        logEntry(methodName, Map.of("year", year));

        try {
            List<BranchDeliveredSummaryDTO> summary = orderRepo.getBranchDeliveredSummary(year);
            logSuccess(methodName, "Retrieved branch delivered summary: " + summary.size() + " branches");
            return summary;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch branch delivered summary due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch branch delivered summary due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    // Get monthly delivered orders for a specific branch
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyDeliveredOrdersByBranch(Long branchId, Integer year) {
        String methodName = "getMonthlyDeliveredOrdersByBranch";
        logEntry(methodName, Map.of("branchId", branchId, "year", year));

        try {
            // Validate branch exists
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

            // Get monthly stats
            List<DeliveredOrderStatsDTO> monthlyStats = orderRepo.getDeliveredOrdersStats(year, null, branchId);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("branchId", branch.getId());
            response.put("branchName", branch.getBranchName());
            response.put("branchCode", branch.getBranchCode());
            response.put("year", year);
            response.put("monthlyStats", monthlyStats);

            // Calculate totals
            Long totalOrders = monthlyStats.stream()
                    .mapToLong(DeliveredOrderStatsDTO::getDeliveredOrdersCount)
                    .sum();
            Long totalProducts = monthlyStats.stream()
                    .mapToLong(DeliveredOrderStatsDTO::getTotalProductsDelivered)
                    .sum();

            response.put("totalDeliveredOrders", totalOrders);
            response.put("totalProductsDelivered", totalProducts);

            logSuccess(methodName, "Retrieved monthly delivered orders for branch: " + branch.getBranchName());
            return response;
        } catch (ResourceNotFoundException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch monthly delivered orders due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch monthly delivered orders due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    // Get top delivered products across all branches
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopDeliveredProducts(Integer year, Integer month, int limit) {
        String methodName = "getTopDeliveredProducts";
        logEntry(methodName, Map.of("year", year, "month", month, "limit", limit));

        try {
            // Get all product stats
            List<ProductDeliveredStatsDTO> productStats = orderRepo.getProductDeliveredStats(year, month, null, null);

            // Group by product and aggregate
            Map<Long, Map<String, Object>> productMap = new HashMap<>();

            for (ProductDeliveredStatsDTO stat : productStats) {
                productMap.computeIfAbsent(stat.getProductId(), k -> {
                    Map<String, Object> productInfo = new HashMap<>();
                    productInfo.put("productId", stat.getProductId());
                    productInfo.put("productName", stat.getProductName());
                    productInfo.put("categoryName", stat.getCategoryName());
                    productInfo.put("totalQuantity", 0L);
                    productInfo.put("branches", new ArrayList<Map<String, Object>>());
                    return productInfo;
                });

                Map<String, Object> productInfo = productMap.get(stat.getProductId());
                Long currentTotal = (Long) productInfo.get("totalQuantity");
                productInfo.put("totalQuantity", currentTotal + stat.getTotalQuantityDelivered());

                // Add branch info
                List<Map<String, Object>> branches = (List<Map<String, Object>>) productInfo.get("branches");
                Map<String, Object> branchInfo = new HashMap<>();
                branchInfo.put("branchId", stat.getBranchId());
                branchInfo.put("branchName", stat.getBranchName());
                branchInfo.put("quantity", stat.getTotalQuantityDelivered());
                branchInfo.put("year", stat.getYear());
                branchInfo.put("month", stat.getMonth());
                branches.add(branchInfo);
            }

            // Sort by total quantity and limit results
            List<Map<String, Object>> topProducts = productMap.values().stream()
                    .sorted((a, b) -> Long.compare(
                            (Long) b.get("totalQuantity"),
                            (Long) a.get("totalQuantity")
                    ))
                    .limit(limit)
                    .collect(Collectors.toList());

            logSuccess(methodName, "Retrieved top " + topProducts.size() + " delivered products");
            return topProducts;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch top delivered products due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch top delivered products due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        }
    }

    private OrderResponseDTO convertToDTO(Order order) {
        try {
            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setId(order.getId());
            dto.setBranchId(order.getBranchId());
            dto.setStatus(order.getStatus());
            dto.setCreatedAt(order.getCreatedAt());

            // Add branch info
            if (order.getBranch() != null) {
                dto.setBranchResponse(convertBranchToDTO(order.getBranch()));
            }

            // Process order items
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                List<OrderItemResponseDTO> itemDTOs = new ArrayList<>();
                Long totalQuantity = 0L;

                for (OrderItem item : order.getOrderItems()) {
                    OrderItemResponseDTO itemDTO = new OrderItemResponseDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setProductId(item.getProduct().getId());
                    itemDTO.setProductName(item.getProduct().getProductName());
                    itemDTO.setQuantity(item.getQuantity());

                    // Create complete product response
                    ProductResponse productResponse = new ProductResponse();
                    productResponse.setId(item.getProduct().getId());
                    productResponse.setProductName(item.getProduct().getProductName());

                    // Set image URL if available
                    if (item.getProduct().getPImage() != null && s3Service != null) {
                        String imageUrl = s3Service.getFileUrl(item.getProduct().getPImage());
                        productResponse.setImageUrl(imageUrl);
                    }

                    // Set category name if available
                    if (item.getProduct().getCategory() != null) {
                        productResponse.setCategoryName(
                                item.getProduct().getCategory().getCategoryName()
                        );
                    }



                    itemDTO.setProductResponse(productResponse);
                    itemDTOs.add(itemDTO);
                    totalQuantity += item.getQuantity();
                }

                dto.setOrderItems(itemDTOs);
                dto.setTotalItems(totalQuantity);
                dto.setProductCount(itemDTOs.size());

            } else {
                dto.setOrderItems(new ArrayList<>());
                dto.setTotalItems(0L);
                dto.setProductCount(0);
            }

            return dto;
        } catch (Exception e) {
            log.error("Error converting order to DTO: orderId={}", order.getId(), e);
            throw new OrderProcessingException("Failed to process order data", e);
        }
    }


    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByDateRange(
            LocalDate fromDate,
            LocalDate toDate,
            int page, int size,
            String sortBy,
            String direction) {

        String methodName = "getOrdersByDateRange";

        // Fix: Create map safely without null values
        Map<String, Object> logParams = new HashMap<>();
        logParams.put("fromDate", fromDate);
        logParams.put("toDate", toDate);
        logParams.put("page", page);
        logParams.put("size", size);

        logEntry(methodName, logParams);
        try {
            // Validate fromDate is required
            if (fromDate == null) {
                throw new ValidationException("From date is required");
            }

            // If toDate is null, set it to fromDate (single day query)
            LocalDate effectiveToDate = (toDate != null) ? toDate : fromDate;

            // Validate date range
            if (effectiveToDate.isBefore(fromDate)) {
                throw new ValidationException("To date cannot be before from date");
            }

            Pageable pageable = createPageable(page, size, sortBy, direction);

            // Get current user for role-based filtering
            User user = getCurrentAuthenticatedUser();

            Page<Order> orderPage;

            // Admin and Godown Manager can see all orders in date range
            if (user.getRole() == Role.ADMIN || user.getRole() == Role.GODOWN_MANAGER) {
                orderPage = orderRepo.findByDateRangeWithUserAndBranch(
                        fromDate.atStartOfDay(),
                        effectiveToDate.plusDays(1).atStartOfDay(), // Include entire toDate day
                        pageable);
            }
            // Manager and Staff can only see their own orders
            else {
                orderPage = orderRepo.findByUserAndDateRangeWithBranch(
                        user.getId(),
                        fromDate.atStartOfDay(),
                        effectiveToDate.plusDays(1).atStartOfDay(), // Include entire toDate day
                        pageable);
            }

            logSuccess(methodName, "Retrieved " + orderPage.getNumberOfElements() +
                    " orders from " + fromDate + " to " + effectiveToDate);

            return orderPage.map(this::convertToDTO);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to fetch orders by date range due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to fetch orders by date range due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new OrderProcessingException(errorMsg, e);
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

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (sortBy == null || sortBy.isEmpty()) sortBy = "createdAt";
        if (direction == null || direction.isEmpty()) direction = "desc";

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

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

    // Enhanced logging methods
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