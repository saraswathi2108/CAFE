package com.anasol.cafe.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProductBranchOrderMatrixDTO {
    private Long productId;
    private String productName;
    private Double warehouseStock;
    private String unit;
    private Map<String, Double> branchOrders = new HashMap<>(); // Key: Branch Name, Value: Total Quantity
}