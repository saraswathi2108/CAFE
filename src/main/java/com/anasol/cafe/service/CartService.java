package com.anasol.cafe.service;

import com.anasol.cafe.dto.AddToCartRequest;
import com.anasol.cafe.dto.CartDTO;
import com.anasol.cafe.dto.CartItemDTO;
import com.anasol.cafe.dto.ProductResponse;
import com.anasol.cafe.entity.Cart;
import com.anasol.cafe.entity.CartItems;
import com.anasol.cafe.entity.Product;
import com.anasol.cafe.entity.User;
import com.anasol.cafe.exceptions.CartProcessingException;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.ValidationException;
import com.anasol.cafe.repository.CartItemsRepository;
import com.anasol.cafe.repository.CartRepository;
import com.anasol.cafe.repository.ProductRepo;
import com.anasol.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemsRepository cartItemsRepository;
    private final UserRepository userRepository;
    private final ProductRepo productRepository;
    private final S3Service s3Service;

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("User not authenticated");
        }

        String email = authentication.getName();
        log.debug("Getting authenticated user with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not Found with email: " + email));
    }

    // Methods for current user (no userId parameter)

    public CartDTO getCartDetails() {
        User user = getCurrentAuthenticatedUser();
        return getCartDetails(user.getId());
    }

    public List<CartItemDTO> getCartItems() {
        User user = getCurrentAuthenticatedUser();
        return getCartItems(user.getId());
    }

    public CartItemDTO addItemToCart(AddToCartRequest request) {
        User user = getCurrentAuthenticatedUser();
        return addItemToCart(user.getId(), request);
    }

    public void removeItemFromCart(Long productId) {
        User user = getCurrentAuthenticatedUser();
        removeItemFromCart(user.getId(), productId);
    }

    public CartItemDTO updateItemQuantity(Long productId, Integer quantity) {
        User user = getCurrentAuthenticatedUser();
        return updateItemQuantity(user.getId(), productId, quantity);
    }

    public void clearCart() {
        User user = getCurrentAuthenticatedUser();
        clearCart(user.getId());
    }

    public Integer getCartItemCount() {
        User user = getCurrentAuthenticatedUser();
        return getCartItemCount(user.getId());
    }

    public Cart getOrCreateCart(Long userId) {
        String methodName = "getOrCreateCart";
        logEntry(methodName, userId);

        try {
            // Try to get cart with items
            Optional<Cart> cartOptional = cartRepository.findByUserIdWithItems(userId);

            if (cartOptional.isPresent()) {
                return cartOptional.get();
            } else {
                // Create new cart
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

                Cart cart = new Cart();
                cart.setUser(user);
                cart.setCartItems(new ArrayList<>());

                Cart savedCart = cartRepository.save(cart);
                logSuccess(methodName, "Created new cart for userId: " + userId);
                return savedCart;
            }
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get/create cart due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get/create cart due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    public CartItemDTO addItemToCart(Long userId, AddToCartRequest request) {
        String methodName = "addItemToCart";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("request", request);
        logEntry(methodName, params);

        try {
            // Validate input
            if (request == null) {
                throw new ValidationException("Add to cart request cannot be null");
            }
            if (request.getProductId() == null) {
                throw new ValidationException("Product ID cannot be null");
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new ValidationException("Quantity must be greater than zero");
            }

            Cart cart = getOrCreateCart(userId);
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

            Optional<CartItems> existingItem = cartItemsRepository
                    .findByCartIdAndProductId(cart.getId(), product.getId());

            CartItems cartItem;
            if (existingItem.isPresent()) {
                cartItem = existingItem.get();
                cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
                log.info("Updated existing cart item: cartId={}, productId={}, newQuantity={}",
                        cart.getId(), product.getId(), cartItem.getQuantity());
            } else {
                cartItem = new CartItems();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setQuantity(request.getQuantity());
                log.info("Created new cart item: cartId={}, productId={}, quantity={}",
                        cart.getId(), product.getId(), request.getQuantity());
            }

            CartItems savedItem = cartItemsRepository.save(cartItem);

            logSuccess(methodName, String.format("Item added to cart: cartId=%d, productId=%d, quantity=%d",
                    cart.getId(), product.getId(), savedItem.getQuantity()));
            return convertToDTO(savedItem);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to add item to cart due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to add item to cart due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(Long userId) {
        String methodName = "getCartItems";
        logEntry(methodName, userId);

        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("No cart found for user id: " + userId));

            List<CartItems> items = cartItemsRepository.findByCartId(cart.getId());

            logSuccess(methodName, "Retrieved " + items.size() + " items for userId: " + userId);
            return items.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get cart items due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get cart items due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    @Transactional(readOnly = true)
    public CartDTO getCartDetails(Long userId) {
        String methodName = "getCartDetails";
        logEntry(methodName, userId);

        try {
            // Use the repository method that eagerly loads items
            Cart cart = cartRepository.findByUserIdWithItemsAndProducts(userId)
                    .orElseGet(() -> {
                        // Create cart if it doesn't exist
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

                        Cart newCart = new Cart();
                        newCart.setUser(user);
                        newCart.setCartItems(new ArrayList<>());

                        Cart savedCart = cartRepository.save(newCart);
                        log.info("Created new cart for user: {}", userId);
                        return savedCart;
                    });

            CartDTO cartDTO = convertCartToDTO(cart);

            logSuccess(methodName, String.format("Cart details retrieved: cartId=%d, itemsCount=%d",
                    cart.getId(), cartDTO.getItems() != null ? cartDTO.getItems().size() : 0));
            return cartDTO;
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get cart details due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get cart details due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }
    public void removeItemFromCart(Long userId, Long productId) {
        String methodName = "removeItemFromCart";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("productId", productId);
        logEntry(methodName, params);

        try {
            if (productId == null) {
                throw new ValidationException("Product ID cannot be null");
            }

            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("No cart found for user id: " + userId));

            cartItemsRepository.deleteByCartIdAndProductId(cart.getId(), productId);

            logSuccess(methodName, String.format("Item removed from cart: cartId=%d, productId=%d",
                    cart.getId(), productId));
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to remove item from cart due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to remove item from cart due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    public CartItemDTO updateItemQuantity(Long userId, Long productId, Integer quantity) {
        String methodName = "updateItemQuantity";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("productId", productId);
        params.put("quantity", quantity);
        logEntry(methodName, params);

        try {
            if (productId == null) {
                throw new ValidationException("Product ID cannot be null");
            }
            if (quantity == null || quantity <= 0) {
                throw new ValidationException("Quantity must be greater than zero");
            }

            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("No cart found for user id: " + userId));

            CartItems cartItem = cartItemsRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

            cartItem.setQuantity(Long.valueOf(quantity));
            CartItems updatedItem = cartItemsRepository.save(cartItem);

            logSuccess(methodName, String.format("Item quantity updated: cartId=%d, productId=%d, newQuantity=%d",
                    cart.getId(), productId, quantity));
            return convertToDTO(updatedItem);
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to update item quantity due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to update item quantity due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    public void clearCart(Long userId) {
        String methodName = "clearCart";
        logEntry(methodName, userId);

        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("No cart found for user id: " + userId));

            cartItemsRepository.findByCartId(cart.getId())
                    .forEach(cartItemsRepository::delete);

            logSuccess(methodName, "Cart cleared for userId: " + userId);
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to clear cart due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to clear cart due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    @Transactional(readOnly = true)
    public Integer getCartItemCount(Long userId) {
        String methodName = "getCartItemCount";
        logEntry(methodName, userId);

        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("No cart found for user id: " + userId));

            int count = cartItemsRepository.countByCartId(cart.getId());

            logSuccess(methodName, "Cart item count: " + count + " for userId: " + userId);
            return count;
        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get cart item count due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get cart item count due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new CartProcessingException(errorMsg);
        }
    }

    private CartDTO convertCartToDTO(Cart cart) {
        try {
            CartDTO dto = new CartDTO();
            dto.setId(cart.getId());
            dto.setUserId(cart.getUser().getId());

            // Initialize items list
            List<CartItemDTO> itemDTOs = new ArrayList<>();

            if (cart.getCartItems() != null) {
                // Use for-each loop to avoid stream issues
                for (CartItems cartItem : cart.getCartItems()) {
                    itemDTOs.add(convertToDTO(cartItem));
                }
            }

            dto.setItems(itemDTOs);

            return dto;
        } catch (Exception e) {
            log.error("Error converting cart to DTO: cartId={}", cart.getId(), e);
            throw new CartProcessingException("Failed to process cart data");
        }
    }

    private CartItemDTO convertToDTO(CartItems cartItem) {
        try {
            CartItemDTO dto = new CartItemDTO();
            dto.setId(cartItem.getId());
            dto.setProductId(cartItem.getProduct().getId());
            dto.setQuantity(cartItem.getQuantity());

            if (cartItem.getProduct() != null) {
                dto.setProductResponse(convertProductToDTO(cartItem.getProduct()));
            }

            return dto;
        } catch (Exception e) {
            log.error("Error converting cart item to DTO: itemId={}", cartItem.getId(), e);
            throw new CartProcessingException("Failed to process cart item data");
        }
    }
    private ProductResponse convertProductToDTO(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        dto.setImageUrl(s3Service.getFileUrl(product.getPImage()));
        dto.setCategoryName(product.getCategory().getCategoryName());
        return dto;
    }



    // Enhanced logging methods matching OrderService
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
}