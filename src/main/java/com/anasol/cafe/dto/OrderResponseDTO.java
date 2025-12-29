package com.anasol.cafe.dto;

import com.anasol.cafe.entity.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponseDTO {
    private Long id;
    private Long productId;
    //private Long userId;
    private Long branchId;
    //private String userEmail;
   // private String branchName;
    private Long quantity;
    private BranchResponse branchResponse;
    private ProductResponse productResponse;
    //private String address;
    private OrderStatus status;
    private LocalDateTime createdAt;
}