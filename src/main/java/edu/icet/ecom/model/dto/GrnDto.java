package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GrnDto {
    private String grnNo;
    private Long supplierId;
    private Double totalAmount;
    private String notes;
    private List<GrnItemDto> items;
}