package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.*;
import edu.icet.ecom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductVariantRepository variantRepository;
    private final StockLogRepository logRepository;
    private final ProductRepository productRepository;
    private final StockReportRepository reportRepository;
    private final SaleRepository saleRepository;
    private final StockBatchRepository stockBatchRepository;

    public List<StockReportEntity> getAllSavedReports() {
        return reportRepository.findAll();
    }

    public List<StockLogEntity> getAllStockLogs() {
        return logRepository.findAllByOrderByLogIdDesc();
    }

    public List<StockBatchEntity> getBatchesByBarcode(String barcodeId) {
        return stockBatchRepository.findByBarcodeIdOrderByRestockDateAsc(barcodeId);
    }

    @Transactional
    public void updateStock(StockUpdateDto dto) {
        ProductVariantEntity variant = variantRepository.findByBarcodeId(dto.getBarcodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        // Increase the quantity
        int currentQty = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        variant.setStockQuantity(currentQty + dto.getQuantityAdded());

        // Determine batch price: use provided newPrice, fall back to priceOverride, then product retailPrice
        Double batchPrice = dto.getNewPrice();
        if (batchPrice == null || batchPrice <= 0) {
            batchPrice = (variant.getPriceOverride() != null && variant.getPriceOverride() > 0)
                    ? variant.getPriceOverride()
                    : (variant.getProduct().getRetailPrice() != null ? variant.getProduct().getRetailPrice() : 0.0);
        }

        // Determine batch price: use provided newPrice, fall back to priceOverride, then product retailPrice
      

// ← DELETED BLOCK WAS HERE. Now go straight to save:
        variantRepository.save(variant);

        // Create a batch record for this restock
        StockBatchEntity batch = new StockBatchEntity();
        batch.setVariant(variant);
        batch.setBarcodeId(variant.getBarcodeId());
        batch.setBatchPrice(batchPrice);
        batch.setQuantityAdded(dto.getQuantityAdded());
        batch.setQuantityRemaining(dto.getQuantityAdded());
        batch.setRestockDate(LocalDateTime.now());
        batch.setNotes(dto.getUpdateReason());
        stockBatchRepository.save(batch);

        // Create a stock log entry
        StockLogEntity log = new StockLogEntity();
        log.setVariant(variant);
        log.setBarcodeId(variant.getBarcodeId());
        log.setQuantityChange(dto.getQuantityAdded());
        log.setUpdateReason(dto.getUpdateReason());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

    /**
     * Deducts sold quantity from FIFO batches (oldest batch first).
     * Called by SaleService when processing a sale.
     */
    @Transactional
    public void deductFromBatches(String barcodeId, int quantityToDeduct) {
        List<StockBatchEntity> batches = stockBatchRepository
                .findByBarcodeIdOrderByRestockDateAsc(barcodeId);

        int remaining = quantityToDeduct;
        for (StockBatchEntity batch : batches) {
            if (remaining <= 0) break;
            int available = batch.getQuantityRemaining() != null ? batch.getQuantityRemaining() : 0;
            if (available <= 0) continue;

            int deduct = Math.min(available, remaining);
            batch.setQuantityRemaining(available - deduct);
            remaining -= deduct;
            stockBatchRepository.save(batch);
        }
    }

    @Transactional
    public StockReportDto generateReport(String type, String date) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = localDate.plusDays(1).atStartOfDay();

        List<SaleEntity> sales = saleRepository.findByDateRange(start, end);
        List<StockLogEntity> logs = logRepository.findByDateRange(start, end);

        int totalIn = 0; int totalOut = 0; double soldValue = 0.0; double discounts = 0.0; double revenue = 0.0;

        for (SaleEntity sale : sales) {
            discounts += (sale.getDiscountAmount() != null) ? sale.getDiscountAmount() : 0.0;
            revenue += (sale.getNetAmount() != null) ? sale.getNetAmount() : 0.0;
        }

        for (StockLogEntity log : logs) {
            int qty = (log.getQuantityChange() != null) ? log.getQuantityChange() : 0;
            if (qty > 0) totalIn += qty;
            else if (qty < 0) {
                totalOut += Math.abs(qty);
                ProductVariantEntity v = log.getVariant();
                if (v != null) {
                    double p = (v.getPriceOverride() != null) ? v.getPriceOverride() :
                            (v.getProduct().getRetailPrice() != null ? v.getProduct().getRetailPrice() : v.getProduct().getWholesalePrice());
                    soldValue += (Math.abs(qty) * p);
                }
            }
        }

        StockReportDto dto = new StockReportDto();
        dto.setReportType(type.toUpperCase());
        dto.setDate(date);
        dto.setTotalItemsIn(totalIn);
        dto.setTotalItemsOut(totalOut);
        dto.setTotalRevenue(revenue);
        dto.setTotalDiscountGiven(discounts);
        dto.setStockValue(calculateStockValue());

        saveOrUpdateReport(dto, soldValue);
        return dto;
    }

    private void saveOrUpdateReport(StockReportDto dto, double soldItemsValue) {
        List<StockReportEntity> existing = reportRepository.findByReportDateAndReportType(dto.getDate(), dto.getReportType());
        StockReportEntity entity = existing.isEmpty() ? new StockReportEntity() : existing.get(0);

        entity.setReportType(dto.getReportType());
        entity.setReportDate(dto.getDate());
        entity.setTotalItemsIn(dto.getTotalItemsIn());
        entity.setTotalItemsOut(dto.getTotalItemsOut());
        entity.setTotalRevenue(dto.getTotalRevenue());
        entity.setTotalDiscountGiven(dto.getTotalDiscountGiven());
        entity.setSoldItemsValue(soldItemsValue);
        entity.setStockValue(dto.getStockValue());
        entity.setGeneratedAt(LocalDateTime.now());
        reportRepository.saveAndFlush(entity);
    }

    private double calculateStockValue() {
        List<StockBatchEntity> activeBatches = stockBatchRepository.findAll().stream()
                .filter(b -> b.getQuantityRemaining() != null && b.getQuantityRemaining() > 0)
                .collect(Collectors.toList());

        // Variants that have batch records
        Set<String> variantsWithBatches = activeBatches.stream()
                .map(b -> b.getVariant().getVariantId())
                .collect(Collectors.toSet());

        // Batch-accurate stock value
        double batchValue = activeBatches.stream()
                .mapToDouble(b -> b.getQuantityRemaining() * (b.getBatchPrice() != null ? b.getBatchPrice() : 0.0))
                .sum();

        // Fallback for variants that were added before the batch system was introduced
        double fallbackValue = productRepository.findAll().stream()
                .flatMap(p -> p.getVariants().stream())
                .filter(v -> !variantsWithBatches.contains(v.getVariantId()))
                .mapToDouble(v -> {
                    int qty = (v.getStockQuantity() != null ? v.getStockQuantity() : 0);
                    double price = (v.getPriceOverride() != null && v.getPriceOverride() > 0)
                            ? v.getPriceOverride()
                            : (v.getProduct().getWholesalePrice() != null ? v.getProduct().getWholesalePrice() : 0.0);
                    return qty * price;
                })
                .sum();

        return batchValue + fallbackValue;
    }

    public void refreshStatus(int productId) {
        productRepository.findById(productId).ifPresent(p -> {
            int stock = p.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
            p.setStockStatus(stock <= 0 ? StockStatus.OUT_OF_STOCK : stock < 10 ? StockStatus.LOW_STOCK : StockStatus.AVAILABLE);
            productRepository.save(p);
        });
    }
}
