package com.anasol.cafe.dto;

import lombok.Data;

@Data
public  class OrderItemDTO {
    private Long productId;
    private String productName;
    private Double quantity;
    private ProductResponse productResponse;
}