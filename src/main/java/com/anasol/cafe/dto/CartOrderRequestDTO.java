package com.anasol.cafe.dto;

import lombok.Data;

@Data
public class CartOrderRequestDTO {
    private Long cartId;
    private Long branchId;
}