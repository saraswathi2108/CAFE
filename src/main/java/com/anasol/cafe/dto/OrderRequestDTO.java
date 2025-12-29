package com.anasol.cafe.dto;

import lombok.Data;

@Data
public class OrderRequestDTO {
    private Long productId;
    //private Long userId;
    private Long branchId;

    private Long quantity;
    //private String address;
}