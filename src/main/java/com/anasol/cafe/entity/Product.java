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
    private Double quantity;

    @Enumerated(EnumType.STRING)
    private NetWeight unit;

    private String pImage;
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public boolean hasSufficientStock(Double requestedQuantity) {
        return this.quantity >= requestedQuantity;
    }

    public void reduceStock(Double requestedQuantity) {
        if (requestedQuantity > this.quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        this.quantity -= requestedQuantity;
    }

    public void increaseStock(Double quantity) {
        this.quantity += quantity;
    }

    public String getFormattedQuantity() {
        if (unit == null) {
            return quantity != null ? String.format("%.0f", quantity) : "0";
        }

        switch (unit) {
            case KILOGRAM:
            case GRAM:
                return String.format("%.2f %s", quantity, unit.getUnit());
            case UNITS:
                return String.format("%.0f %s", quantity, unit.getUnit());
            default:
                return String.format("%.0f", quantity);
        }
    }


}
