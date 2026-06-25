package edu.icet.ecom.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.enums.ProductUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "product")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    private String productName;
    private String category;
    private Double wholesalePrice;
    private Double retailPrice;

    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus;

    @Enumerated(EnumType.STRING)
    private ProductUnit unit;

    private String currency;
    private Double discountPercentage;
    private String material;
    private String season;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplier;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("product")
    private List<ProductVariantEntity> variants;
}