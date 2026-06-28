package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreditSettlementDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private Double amountPaid;
    private String paymentMethod;
    private String referenceNote;
    private LocalDateTime settlementDate;
}