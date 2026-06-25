package edu.icet.ecom.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "stock_batch")
public class StockBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnoreProperties({"stockLogs", "stockBatches", "product", "sku", "priceOverride", "barcodeId", "size", "color", "stockQuantity"})
    private ProductVariantEntity variant;

    private String barcodeId;
    private Double batchPrice;
    private Integer quantityAdded;
    private Integer quantityRemaining;
    private LocalDateTime restockDate;
    private String notes;
}