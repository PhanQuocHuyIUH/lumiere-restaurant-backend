package iuh.fit.se.analytics.api;

import iuh.fit.se.analytics.api.dto.AnalyticsSummaryResponse;
import iuh.fit.se.analytics.api.dto.RevenueDetailResponse;
import iuh.fit.se.analytics.application.AnalyticsService;
import iuh.fit.se.shared.response.ApiResponse;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@PreAuthorize("hasRole('MANAGER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary(
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate
    ) {
        AnalyticsSummaryResponse response = analyticsService.getSummary(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueDetailResponse>> getRevenueDetail(
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "groupBy", defaultValue = "DAY") GroupBy groupBy
    ) {
        RevenueDetailResponse response = analyticsService.getRevenueDetail(fromDate, toDate, groupBy);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}