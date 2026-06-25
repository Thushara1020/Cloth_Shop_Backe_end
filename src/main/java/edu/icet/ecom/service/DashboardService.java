package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.model.dto.DashboardAnalyticsDto;
import edu.icet.ecom.model.entity.SaleEntity;
import edu.icet.ecom.repository.ProductRepository;
import edu.icet.ecom.repository.ProductVariantRepository;
import edu.icet.ecom.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final SaleRepository saleRepository;

    public DashboardAnalyticsDto getDashboardAnalytics() {
        DashboardAnalyticsDto dto = new DashboardAnalyticsDto();

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<SaleEntity> todaysSales = saleRepository.findByDateRange(startOfDay, endOfDay);

        double todayRevenue = todaysSales.stream()
                .mapToDouble(sale -> sale.getNetAmount() != null ? sale.getNetAmount() : 0.0)
                .sum();
        dto.setTodayRevenue(todayRevenue);

        double todayDiscount = todaysSales.stream()
                .mapToDouble(sale -> sale.getDiscountAmount() != null ? sale.getDiscountAmount() : 0.0)
                .sum();
        dto.setTodayDiscountGiven(todayDiscount);

        dto.setTotalProductsCount((int) productRepository.count());

        long lowStockCount = productRepository.findAll().stream()
                .filter(p -> p.getStockStatus() == StockStatus.LOW_STOCK)
                .count();
        dto.setLowStockAlertsCount((int) lowStockCount);

        int totalPhysicalStock = variantRepository.findAll().stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();
        dto.setPhysicalStockCount(totalPhysicalStock);

        double invValue = productRepository.findAll().stream()
                .mapToDouble(p -> {
                    double retailPrice = p.getRetailPrice() != null ? p.getRetailPrice() : 0.0;
                    int totalQty = p.getVariants() != null
                            ? p.getVariants().stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum()
                            : 0;
                    return retailPrice * totalQty;
                }).sum();
        dto.setInventoryValue(invValue);

        dto.setTotalOutstandingAmount(0.0);

        return dto;
    }
}