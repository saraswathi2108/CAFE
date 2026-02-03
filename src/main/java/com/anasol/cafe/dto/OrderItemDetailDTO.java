package com.anasol.cafe.dto;

import lombok.Data;

@Data
public class OrderItemDetailDTO {
    private Long productId;
    private String productName;
    private Double quantity;
    private ProductResponse productResponse; // If you need full product details
}