package com.anasol.cafe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchDeliveredSummaryDTO {
    private Long branchId;
    private String branchName;
    private String branchCode;
    private Long totalDeliveredOrders;
    private Long totalProductsDelivered;
}