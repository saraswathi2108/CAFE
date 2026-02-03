package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;

    private NetWeight unit;
    private String formattedQuantity;

    private Double quantity;

    private ProductResponse productResponse;


}
