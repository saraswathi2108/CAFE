package com.anasol.cafe.dto;

import com.anasol.cafe.entity.NetWeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String productName;
    private Double quantity;
    private NetWeight unit;

    private String imageUrl;
    private String categoryName;
}
