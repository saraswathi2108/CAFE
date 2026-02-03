package com.anasol.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Double quantity;

    @Enumerated(EnumType.STRING)
    private NetWeight unit;

    // You can add price field later if needed
    // private BigDecimal price;
    // private BigDecimal subtotal;
}