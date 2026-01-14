package com.anasol.cafe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveredOrderStatsDTO {
    private Long branchId;
    private String branchName;
    private String branchCode;
    private Integer year;
    private Integer month;
    private String monthName;
    private Long deliveredOrdersCount;
    private Long totalProductsDelivered;
}