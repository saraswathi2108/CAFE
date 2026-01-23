package com.anasol.cafe.dto;

import lombok.Data;

@Data
public class OrderEditResponseDTO {
    private OrderResponseDTO order;
    private EditSummaryDTO editSummary;
    
    @Data
    public static class EditSummaryDTO {
        private Long orderId;
        private Long productId;
        private String productName;
        private Double oldQuantity;
        private String oldUnit;
        private Double newQuantity;
        private String newUnit;
        private Double quantityDifference;
        private String editReason;
        private String status;
        
        public EditSummaryDTO(OrderEditRequestDTO editRequest, String productName) {
            this.orderId = editRequest.getOrderId();
            this.productId = editRequest.getProductId();
            this.productName = productName;
            this.oldQuantity = editRequest.getOldQuantity();
            this.oldUnit = editRequest.getOldUnit() != null ? editRequest.getOldUnit().name() : null;
            this.newQuantity = editRequest.getNewQuantity();
            this.newUnit = editRequest.getUnit() != null ? editRequest.getUnit().name() : null;
            this.quantityDifference = editRequest.getQuantityDifference();
            this.editReason = editRequest.getEditReason();
            this.status = "SUCCESS";
        }
    }
}