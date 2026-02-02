package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EditOrderProductDTO {
    private Double newQuantity;
    private NetWeight newUnit;
    private String reason;
}