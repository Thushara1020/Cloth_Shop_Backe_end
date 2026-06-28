package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardAnalyticsDto {
    private Double todayRevenue;
    private Double todayDiscountGiven;
    private Double inventoryValue;
    private Integer lowStockAlertsCount;
    private Integer totalProductsCount;
    private Integer physicalStockCount;
    private Double totalOutstandingAmount;
}