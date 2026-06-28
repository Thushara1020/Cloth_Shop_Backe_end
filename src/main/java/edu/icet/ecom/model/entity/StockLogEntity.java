package edu.icet.ecom.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.icet.ecom.enums.SaleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "stock_log")
public class StockLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("LOG_ID")
    private Long logId;

    @JsonProperty("BARCODE_ID")
    private String barcodeId;

    @JsonProperty("QUANTITY_CHANGE")
    private Integer quantityChanged;

    @JsonProperty("TIMESTAMP")
    private LocalDateTime timestamp;

    @JsonProperty("UPDATE_REASON")
    private String updateReason;
    @Enumerated(EnumType.STRING)   // ← stores "RETAIL" / "WHOLESALE" as text
    @Column(name = "SALE_TYPE")    // ← must match your DB column name
    private SaleType saleType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id")
    @JsonProperty("ADMIN_ID")
    @JsonIgnoreProperties({"password", "email", "username"}) // Only show ID if needed
    private AdminEntity admin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonProperty("VARIANT_ID")
    @JsonIgnoreProperties({"stockLogs", "product", "sku", "priceOverride"}) // Only show the UUID/ID
    private ProductVariantEntity variant;
}