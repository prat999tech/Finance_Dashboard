package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.MonthlyTrendResponse;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
@Tag(name = "Dashboard", description = "Aggregated analytics for ANALYST and ADMIN roles")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Overall summary: totals, net balance, category breakdowns")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/monthly-trends")
    @Operation(summary = "Month-by-month income vs expense over the last N months")
    public ResponseEntity<MonthlyTrendResponse> getMonthlyTrends(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(dashboardService.getMonthlyTrends(months));
    }

    @GetMapping("/recent")
    @Operation(summary = "The 10 most recent transactions")
    public ResponseEntity<List<TransactionResponse>> getRecent() {
        return ResponseEntity.ok(dashboardService.getRecentTransactions());
    }
}
