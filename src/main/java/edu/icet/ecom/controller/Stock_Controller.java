package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.StockBatchEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.model.entity.StockReportEntity;
import edu.icet.ecom.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@CrossOrigin
public class Stock_Controller {

    private final StockService stockService;

    @PostMapping("/update")
    public ResponseEntity<String> updateStock(@RequestBody StockUpdateDto stockUpdateDto) {
        stockService.updateStock(stockUpdateDto);
        return ResponseEntity.ok("Stock updated successfully");
    }

    @GetMapping("/reports/daily")
    public ResponseEntity<StockReportDto> getDaily(@RequestParam String date) {
        return ResponseEntity.ok(stockService.generateReport("Daily", date));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<StockReportDto> getMonthly(@RequestParam String date) {
        return ResponseEntity.ok(stockService.generateReport("Monthly", date));
    }

    @GetMapping("/reports/Yearly")
    public ResponseEntity<StockReportDto> getYearly(@RequestParam String date) {
        return ResponseEntity.ok(stockService.generateReport("Yearly", date));
    }

    @GetMapping("/reports/all")
    public ResponseEntity<List<StockReportEntity>> getAllReports() {
        return ResponseEntity.ok(stockService.getAllSavedReports());
    }

    @GetMapping("/logs/all")
    public ResponseEntity<List<StockLogEntity>> getAllLogs() {
        return ResponseEntity.ok(stockService.getAllStockLogs());
    }

    /**
     * Returns batch data as plain Maps — avoids Hibernate lazy proxy
     * serialization errors (ByteBuddyInterceptor / hibernateLazyInitializer).
     */
    @GetMapping("/batches/by-barcode")
    public ResponseEntity<List<Map<String, Object>>> getBatchesByBarcode(@RequestParam String barcodeId) {
        List<StockBatchEntity> batches = stockService.getBatchesByBarcode(barcodeId);

        List<Map<String, Object>> result = batches.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("batchId",           b.getBatchId());
            map.put("barcodeId",         b.getBarcodeId() != null ? b.getBarcodeId() : "");
            map.put("batchPrice",        b.getBatchPrice() != null ? b.getBatchPrice() : 0.0);
            map.put("quantityAdded",     b.getQuantityAdded() != null ? b.getQuantityAdded() : 0);
            map.put("quantityRemaining", b.getQuantityRemaining() != null ? b.getQuantityRemaining() : 0);
            map.put("restockDate",       b.getRestockDate() != null ? b.getRestockDate().toString() : "");
            map.put("notes",             b.getNotes() != null ? b.getNotes() : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}