package com.anasol.cafe.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartOrderResponseDTO {
    private String message;
    private int totalOrders;
    private List<OrderResponseDTO> orders;
}