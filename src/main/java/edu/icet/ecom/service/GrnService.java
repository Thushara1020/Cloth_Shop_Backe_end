package edu.icet.ecom.service;

import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.GrnDto;
import edu.icet.ecom.model.dto.GrnItemDto;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockBatchEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.model.entity.SupplierEntity;
import edu.icet.ecom.repository.ProductVariantRepository;
import edu.icet.ecom.repository.StockBatchRepository;
import edu.icet.ecom.repository.StockLogRepository;
import edu.icet.ecom.repository.SupplierRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GrnService {

    private final ProductVariantRepository variantRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SupplierRepository supplierRepository;
    private final StockLogRepository stockLogRepository;

    @Transactional
    public void processGrn(GrnDto grnDto) {
        if (grnDto.getSupplierId() != null) {
            SupplierEntity supplier = supplierRepository.findById(grnDto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }

        for (GrnItemDto item : grnDto.getItems()) {
            ProductVariantEntity variant = variantRepository.findByBarcodeId(item.getBarcodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product with barcode " + item.getBarcodeId() + " not found"));

            int existingStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
            int newStock = existingStock + item.getQuantity();
            variant.setStockQuantity(newStock);
            variantRepository.save(variant);


            StockBatchEntity batch = new StockBatchEntity();
            batch.setVariant(variant);
            batch.setBarcodeId(item.getBarcodeId());
            batch.setQuantityAdded(item.getQuantity());
            batch.setQuantityRemaining(item.getQuantity());
            batch.setBatchPrice(item.getSellingPrice());
            batch.setRestockDate(LocalDateTime.now());
            batch.setNotes(grnDto.getNotes());

            stockBatchRepository.save(batch);


            StockLogEntity log = new StockLogEntity();
            log.setVariant(variant);
            log.setBarcodeId(item.getBarcodeId());
            log.setQuantityChanged(item.getQuantity());
            log.setUpdateReason("VIA_GRN");
            log.setTimestamp(LocalDateTime.now());

            stockLogRepository.save(log);
        }
    }
}