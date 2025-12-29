package com.anasol.cafe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    private List<T> content;
    private int currentPage;
    private long totalItems;
    private int totalPages;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setCurrentPage(page.getNumber());
        response.setTotalItems(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setPageSize(page.getSize());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }
}