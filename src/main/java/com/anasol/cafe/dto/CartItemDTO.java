package com.anasol.cafe.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;

    private Long quantity;

    private ProductResponse productResponse;


}
