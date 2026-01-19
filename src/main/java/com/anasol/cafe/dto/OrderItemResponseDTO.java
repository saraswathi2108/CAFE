package com.anasol.cafe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderItemResponseDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Double quantity;
    @JsonProperty("productResponse")
    private ProductResponse productResponse;
}