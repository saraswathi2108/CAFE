package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import lombok.Data;


@Data
public class OrderEditRequestDTO {

    private Long orderId;

    private Long productId;


    private Double newQuantity;

    private NetWeight unit; // Unit for the new quantity
    private String editReason;

    // Optional fields for response
    private Double oldQuantity;
    private NetWeight oldUnit;
    private Double quantityDifference;
}