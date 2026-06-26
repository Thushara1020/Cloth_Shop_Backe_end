package edu.icet.ecom.service;

import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.SalesDto;
import edu.icet.ecom.model.dto.SalesItemDto;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.*;
import edu.icet.ecom.repository.*;
import io.micrometer.common.lang.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SaleService {

    private final SalesItemRepository salesItemRepository;
    private final SaleRepository saleRepository;
    private final ProductVariantRepository variantRepository;
    private final StockLogRepository logRepository;
    private final AdminRepository adminRepository;
    private final StockService stockService;
    private final StockReportRepository stockReportRepository;

    public List<SalesDto> getAllSales() {
        return saleRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<SalesDto> findSalesByBarcode(String barcodeId) {
        List<SalesItemEntity> items = salesItemRepository.findByBarcodeId(barcodeId);

        return items.stream()
                .map(item -> item.getSale())
                .distinct()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private SalesDto convertToDto(SaleEntity entity) {
        SalesDto dto = new SalesDto();

        dto.setSaleId(entity.getSaleId());
        dto.setSaleType(entity.getSaleType());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setDiscountPercentage(entity.getDiscountPercentage());
        dto.setDiscountedApplied(entity.getDiscountAmount());
        dto.setNetAmount(entity.getNetAmount());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setTimestamp(entity.getTimestamp());

        if (entity.getAdmin() != null) {
            dto.setAdminId(entity.getAdmin().getAdminId());
            dto.setSalesPersonName(
                    entity.getAdmin().getFullName() != null
                            ? entity.getAdmin().getFullName()
                            : entity.getAdmin().getUsername()
            );
        }

        if (entity.getItems() != null) {
            List<SalesItemDto> itemDtos = entity.getItems().stream().map(itemEntity -> {
                SalesItemDto itemDto = new SalesItemDto();
                itemDto.setBarcodeId(itemEntity.getBarcodeId());
                itemDto.setQuantity(itemEntity.getQuantity());
                itemDto.setUnitPrice(itemEntity.getUnitPrice());
                itemDto.setTotalPrice(itemEntity.getTotalPrice());

                if (itemEntity.getVariant() != null && itemEntity.getVariant().getProduct() != null) {
                    itemDto.setItemName(itemEntity.getVariant().getProduct().getProductName());
                    itemDto.setVarientId(itemEntity.getVariant().getVariantId());
                }

                return itemDto;
            }).collect(Collectors.toList());

            dto.setItems(itemDtos);
        }

        return dto;
    }

    @Transactional
    public void placeOrder(SalesDto salesDto) {
        if (salesDto.getItems() == null || salesDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with no items.");
        }
        boolean hasWholesale = false;
        boolean hasRetail = false;

        for (SalesItemDto item : salesDto.getItems()) {
            if (item.getQuantity() >= 6) {
                hasWholesale = true;
            } else {
                hasRetail = true;
            }
        }

        edu.icet.ecom.enums.SaleType determinedType;
        if (hasWholesale && hasRetail) {
            determinedType = edu.icet.ecom.enums.SaleType.MIXED;
        } else if (hasWholesale) {
            determinedType = edu.icet.ecom.enums.SaleType.WHOLESALE;
        } else {
            determinedType = edu.icet.ecom.enums.SaleType.RETAIL;
        }

        SaleEntity saleEntity = new SaleEntity();
        saleEntity.setSaleId("SALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        saleEntity.setSaleType(determinedType);

        LocalDateTime now = LocalDateTime.now();
        saleEntity.setTimestamp(now);
        saleEntity.setPaymentMethod(salesDto.getPaymentMethod());

        AdminEntity admin = adminRepository.findById(salesDto.getAdminId())
                .orElseThrow(() -> new EntityNotFoundException("Admin ID " + salesDto.getAdminId() + " not found."));

        saleEntity.setAdmin(admin);

        double totalAmount = 0.0;
        double totalDiscountAmount = 0.0;
        List<SalesItemEntity> itemEntities = new ArrayList<>();

        for (SalesItemDto itemDto : salesDto.getItems()) {
            ProductVariantEntity variant = variantRepository
                    .findByBarcodeId(itemDto.getBarcodeId())
                    .orElseThrow(() -> new RuntimeException("Barcode not found: " + itemDto.getBarcodeId()));

            if (variant.getStockQuantity() < itemDto.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + variant.getSku());
            }

            variant.setStockQuantity(variant.getStockQuantity() - itemDto.getQuantity());
            variantRepository.saveAndFlush(variant);

            stockService.deductFromBatches(variant.getBarcodeId(), itemDto.getQuantity());

            double unitPrice = itemDto.getUnitPrice();
            int qty = itemDto.getQuantity();
            double itemTotal = unitPrice * qty;
            double pct = (salesDto.getDiscountPercentage() != null) ? salesDto.getDiscountPercentage() : 0.0;
            double itemDiscount = itemTotal * (pct / 100);
            double itemNet = itemTotal - itemDiscount;

            SalesItemEntity itemEntity = new SalesItemEntity();
            itemEntity.setSale(saleEntity);
            itemEntity.setVariant(variant);
            itemEntity.setBarcodeId(variant.getBarcodeId());
            itemEntity.setQuantity(qty);
            itemEntity.setUnitPrice(unitPrice);
            itemEntity.setTotalPrice(itemTotal);
            itemEntity.setDiscountAmount(itemDiscount);
            itemEntity.setNetPrice(itemNet);
            itemEntities.add(itemEntity);

            totalAmount += itemTotal;
            totalDiscountAmount += itemDiscount;

            StockLogEntity log = new StockLogEntity();
            log.setVariant(variant);
            log.setBarcodeId(variant.getBarcodeId());
            log.setQuantityChanged(-qty);
            log.setSaleType(qty >= 6 ? edu.icet.ecom.enums.SaleType.WHOLESALE : edu.icet.ecom.enums.SaleType.RETAIL);
            String adminName = (admin.getFullName() != null) ? admin.getFullName() : admin.getUsername();
            log.setUpdateReason("SALE BY: " + adminName);
            log.setTimestamp(now);
            log.setAdmin(admin);
            logRepository.saveAndFlush(log);

            stockService.refreshStatus(variant.getProduct().getProductId());
        }

        saleEntity.setTotalAmount(totalAmount);
        saleEntity.setDiscountPercentage(salesDto.getDiscountPercentage() != null ? salesDto.getDiscountPercentage() : 0.0);
        saleEntity.setDiscountAmount(totalDiscountAmount);
        saleEntity.setNetAmount(totalAmount - totalDiscountAmount);
        saleEntity.setItems(itemEntities);

        saleRepository.saveAndFlush(saleEntity);
        stockService.generateReport("DAILY", now.toLocalDate().toString());
    }

    public @Nullable StockReportDto generateReport(String type, String date) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = localDate.plusDays(1).atStartOfDay();

        List<SaleEntity> sales = saleRepository.findByDateRange(start, end);
        List<StockLogEntity> logs = logRepository.findByDateRange(start, end);

        if (sales.isEmpty() && logs.isEmpty()) {
            return null;
        }

        int itemsOut = 0;
        int itemsIn = 0;
        double revenue = 0.0;
        double totalDiscount = 0.0;

        for (SaleEntity sale : sales) {
            revenue += (sale.getNetAmount() != null) ? sale.getNetAmount() : 0.0;
            totalDiscount += (sale.getDiscountAmount() != null) ? sale.getDiscountAmount() : 0.0;

            if (sale.getItems() != null) {
                itemsOut += sale.getItems().stream()
                        .mapToInt(SalesItemEntity::getQuantity)
                        .sum();
            }
        }

        for (StockLogEntity log : logs) {
            if (log.getQuantityChanged() != null && log.getQuantityChanged() > 0) {
                itemsIn += log.getQuantityChanged();
            }
        }

        double currentStockValue = variantRepository.findAll().stream()
                .mapToDouble(v -> {
                    int qty = (v.getStockQuantity() != null ? v.getStockQuantity() : 0);
                    double price = (v.getPriceOverride() != null && v.getPriceOverride() > 0)
                            ? v.getPriceOverride()
                            : (v.getProduct().getWholesalePrice() != null ? v.getProduct().getWholesalePrice() : 0.0);
                    return qty * price;
                })
                .sum();
        return createAndSaveReport(type, date, itemsIn, itemsOut, revenue, totalDiscount, currentStockValue);
    }

    private StockReportDto createAndSaveReport(String type, String date, int in, int out, double rev, double disc, double val) {
        StockReportEntity reportEntity = new StockReportEntity();
        reportEntity.setReportType(type.toUpperCase());
        reportEntity.setReportDate(date);
        reportEntity.setTotalItemsIn(in);
        reportEntity.setTotalItemsOut(out);
        reportEntity.setTotalRevenue(rev);
        reportEntity.setTotalDiscountGiven(disc);
        reportEntity.setStockValue(val);
        reportEntity.setGeneratedAt(LocalDateTime.now());
        stockReportRepository.save(reportEntity);

        StockReportDto dto = new StockReportDto();
        dto.setReportType(reportEntity.getReportType());
        dto.setDate(reportEntity.getReportDate());
        dto.setTotalItemsIn(in);
        dto.setTotalItemsOut(out);
        dto.setTotalRevenue(rev);
        dto.setTotalDiscountGiven(disc);
        dto.setStockValue(val);
        return dto;
    }

    @Transactional
    public void processReturn(String saleId, String barcodeId, Integer returnQty) {
        List<SalesItemEntity> items = salesItemRepository.findBySale_SaleIdAndBarcodeId(saleId, barcodeId);

        if (items.isEmpty()) throw new RuntimeException("Item not found in this sale.");

        SalesItemEntity item = items.get(0);

        ProductVariantEntity variant = item.getVariant();
        variant.setStockQuantity(variant.getStockQuantity() + returnQty);
        variantRepository.save(variant);

        StockLogEntity log = new StockLogEntity();
        log.setVariant(variant);
        log.setBarcodeId(barcodeId);
        log.setQuantityChanged(returnQty);
        log.setUpdateReason("RETURNED FROM SALE: " + saleId);
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

    @Transactional
    public void processReturn(Map<String, Object> returnRequest) {
        String saleId = (String) returnRequest.get("saleId");
        String barcodeId = (String) returnRequest.get("barcodeId");

        Object qtyObj = returnRequest.get("quantity");
        Integer quantityToReturn = (qtyObj instanceof Number) ? ((Number) qtyObj).intValue() : 0;

        SaleEntity sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));

        SalesItemEntity item = sale.getItems().stream()
                .filter(i -> i.getBarcodeId().equals(barcodeId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in this sale"));

        if (quantityToReturn > item.getQuantity()) {
            throw new RuntimeException("Return quantity exceeds original purchased quantity");
        }

        StockUpdateDto stockUpdate = new StockUpdateDto();
        stockUpdate.setBarcodeId(barcodeId);
        stockUpdate.setVarientId(item.getVariant().getVariantId());
        stockUpdate.setQuantityAdded(quantityToReturn);
        stockUpdate.setUpdateReason("RETURNED FROM SALE: " + saleId);
        stockUpdate.setDate(LocalDateTime.now().toString());

        stockService.updateStock(stockUpdate);

        item.setQuantity(item.getQuantity() - quantityToReturn);
        item.setTotalPrice(item.getUnitPrice() * item.getQuantity());

        double newTotal = sale.getItems().stream()
                .mapToDouble(SalesItemEntity::getTotalPrice)
                .sum();

        sale.setTotalAmount(newTotal);

        double discPct = (sale.getDiscountPercentage() != null) ? sale.getDiscountPercentage() : 0.0;
        double newDiscAmount = newTotal * (discPct / 100);

        sale.setDiscountAmount(newDiscAmount);
        sale.setNetAmount(newTotal - newDiscAmount);

        saleRepository.save(sale);
    }
}