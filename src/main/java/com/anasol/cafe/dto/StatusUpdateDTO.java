package com.anasol.cafe.dto;

import com.anasol.cafe.entity.OrderStatus;
import lombok.Data;

@Data
public class StatusUpdateDTO {
    private OrderStatus status;
}