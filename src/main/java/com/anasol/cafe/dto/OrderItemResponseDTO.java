package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderItemResponseDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Double quantity;
    private NetWeight unit;
    @JsonProperty("productResponse")
    private ProductResponse productResponse;
}