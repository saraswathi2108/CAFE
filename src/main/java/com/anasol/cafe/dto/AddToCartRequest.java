package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class AddToCartRequest {
    private Long productId;
    private Double quantity ;

    private NetWeight unit;
}
