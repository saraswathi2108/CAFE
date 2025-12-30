package com.anasol.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private Long quantity;

    private String pImage;
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public boolean hasSufficientStock(Long requestedQuantity) {
        return this.quantity >= requestedQuantity;
    }

    public void reduceStock(Long requestedQuantity) {
        if (requestedQuantity > this.quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        this.quantity -= requestedQuantity;
    }

    public void increaseStock(Long quantity) {
        this.quantity += quantity;
    }


}
