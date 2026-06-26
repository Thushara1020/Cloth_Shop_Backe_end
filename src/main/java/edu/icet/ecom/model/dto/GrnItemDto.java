package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrnItemDto {
    private String barcodeId;
    private Double costPrice;
    private Double sellingPrice;
    private Integer quantity;
    private Double discountPercentage;
}