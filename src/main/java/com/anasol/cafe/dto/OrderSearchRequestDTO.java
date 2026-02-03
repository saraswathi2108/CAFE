// Create a new DTO class for search parameters
package com.anasol.cafe.dto;

import com.anasol.cafe.entity.OrderStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class OrderSearchRequestDTO {
    private Long orderId;
    private String branchName;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
    
    private OrderStatus status;
    private String customerName;
    private String customerEmail;
    
    // Default values
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String direction = "desc";
}