package com.anasol.cafe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeliveredStatsDTO {
    private Long productId;
    private String productName;
    private String categoryName;
    private Long branchId;
    private String branchName;
    private Integer year;
    private Integer month;
    private String monthName;
    private Double totalQuantityDelivered;
}
