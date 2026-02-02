package com.anasol.cafe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "cart")
@AllArgsConstructor
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // @ManyToOne
    // @JoinColumn(name = "product_id")
    // private Order order;


    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CartItems> cartItems = new ArrayList<>();

    // Helper method to add item
    public void addCartItem(CartItems item) {
        cartItems.add(item);
        item.setCart(this);
    }

    // Helper method to remove item
    public void removeCartItem(CartItems item) {
        cartItems.remove(item);
        item.setCart(null);
    }
}