package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import lombok.Data;

@Data
public class EditOrderQuantityDTO {
    private Double newQuantity;
    private NetWeight newUnit;
    private Long productId;
    private Long orderId;

}