package com.anasol.cafe.dto;

import com.anasol.cafe.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDTO {
    private Long id;
    private Long branchId;
    private BranchResponse branchResponse;
    private OrderStatus status;
    private LocalDateTime createdAt;

    @JsonProperty("items")
    private List<OrderItemResponseDTO> orderItems;

    private Double totalItems;
    private Integer productCount;
}