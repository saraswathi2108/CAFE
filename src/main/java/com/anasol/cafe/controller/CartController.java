package com.anasol.cafe.controller;

import com.anasol.cafe.dto.AddToCartRequest;
import com.anasol.cafe.dto.CartDTO;
import com.anasol.cafe.dto.CartItemDTO;
import com.anasol.cafe.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

public class CartController {

    private final CartService cartService;

    @GetMapping("/user")

    public ResponseEntity<CartDTO> getMyCart() {
        // User will be identified from authentication token
        CartDTO cartDTO = cartService.getCartDetails();
        return ResponseEntity.ok(cartDTO);
    }

    @GetMapping("/user/items")
    public ResponseEntity<List<CartItemDTO>> getMyCartItems() {
        // User will be identified from authentication token
        List<CartItemDTO> items = cartService.getCartItems();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/user/items")
    public ResponseEntity<CartItemDTO> addToMyCart(
            @Valid @RequestBody AddToCartRequest request) {

        // User will be identified from authentication token
        CartItemDTO cartItemDTO = cartService.addItemToCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartItemDTO);
    }

    @DeleteMapping("/user/items/{productId}")
    public ResponseEntity<Void> removeFromMyCart(@PathVariable Long productId) {

        // User will be identified from authentication token
        cartService.removeItemFromCart(productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/items")
    public ResponseEntity<Void> clearMyCart() {


        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/count")
    public ResponseEntity<Integer> getMyCartItemCount() {

        // User will be identified from authentication token
        Integer count = cartService.getCartItemCount();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/user/items/{productId}/quantity")
    public ResponseEntity<CartItemDTO> updateMyItemQuantity(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        // User will be identified from authentication token
        CartItemDTO updatedItem = cartService.updateItemQuantity(productId, quantity);
        return ResponseEntity.ok(updatedItem);
    }

    // ADMIN ENDPOINTS (for managing other users' carts)

//    @GetMapping("/users/{userId}")
//    @PreAuthorize("hasRole('ADMIN')")
//
//    public ResponseEntity<CartDTO> getUserCart(@PathVariable Long userId) {
//        CartDTO cartDTO = cartService.getCartDetails(userId);
//        return ResponseEntity.ok(cartDTO);
//    }
//
//    @GetMapping("/users/{userId}/items")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Get items in specific user's cart (Admin only)")
//    public ResponseEntity<List<CartItemDTO>> getUserCartItems(@PathVariable Long userId) {
//        List<CartItemDTO> items = cartService.getCartItems(userId);
//        return ResponseEntity.ok(items);
//    }
//
//    @PostMapping("/users/{userId}/items")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Add item to specific user's cart (Admin only)")
//    public ResponseEntity<CartItemDTO> addToUserCart(
//            @PathVariable Long userId,
//            @Valid @RequestBody AddToCartRequest request) {
//
//        CartItemDTO cartItemDTO = cartService.addItemToCart(userId, request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(cartItemDTO);
//    }
//
//    @DeleteMapping("/users/{userId}/items/{productId}")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Remove item from specific user's cart (Admin only)")
//    public ResponseEntity<Void> removeFromUserCart(
//            @PathVariable Long userId,
//            @PathVariable Long productId) {
//
//        cartService.removeItemFromCart(userId, productId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @DeleteMapping("/users/{userId}/items")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Clear specific user's cart (Admin only)")
//    public ResponseEntity<Void> clearUserCart(@PathVariable Long userId) {
//        cartService.clearCart(userId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/users/{userId}/count")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Get item count in specific user's cart (Admin only)")
//    public ResponseEntity<Integer> getUserCartItemCount(@PathVariable Long userId) {
//        Integer count = cartService.getCartItemCount(userId);
//        return ResponseEntity.ok(count);
//    }
//
//    @PutMapping("/users/{userId}/items/{productId}/quantity")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "Update item quantity in specific user's cart (Admin only)")
//    public ResponseEntity<CartItemDTO> updateUserItemQuantity(
//            @PathVariable Long userId,
//            @PathVariable Long productId,
//            @RequestParam Integer quantity) {
//
//        CartItemDTO updatedItem = cartService.updateItemQuantity(userId, productId, quantity);
//        return ResponseEntity.ok(updatedItem);
//    }
}