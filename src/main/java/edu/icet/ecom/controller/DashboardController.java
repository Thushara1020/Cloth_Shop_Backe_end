package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.DashboardAnalyticsDto;
import edu.icet.ecom.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin()
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/analytics")
    public ResponseEntity<DashboardAnalyticsDto> getAnalytics(
            @RequestParam(value = "period", defaultValue = "today") String period) {

        return ResponseEntity.ok(dashboardService.getDashboardAnalytics(period));
    }
}