package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.StockBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatchEntity, Long> {

    List<StockBatchEntity> findByBarcodeIdOrderByRestockDateAsc(String barcodeId);

    List<StockBatchEntity> findByVariant_VariantIdOrderByRestockDateAsc(String variantId);
}