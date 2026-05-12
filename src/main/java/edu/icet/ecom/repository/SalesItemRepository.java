package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.SalesItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SalesItemRepository extends JpaRepository<SalesItemEntity, Long> {

    // Find all sale entries where this specific barcode was sold
    List<SalesItemEntity> findByBarcodeId(String barcodeId);

    // Find the specific item record if you already have the Sale ID and Barcode
    List<SalesItemEntity> findBySale_SaleIdAndBarcodeId(String saleId, String barcodeId);
}