package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantDto {
    private String variantId;

    private String dimensions;
    private String materialOrType;

    private Integer stockQuantity;
    private String sku;
    private String barcodeId;
    private Double priceOverride;
    private Double finalPrice;
}