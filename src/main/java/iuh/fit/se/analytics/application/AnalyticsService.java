package iuh.fit.se.analytics.application;

import iuh.fit.se.analytics.api.GroupBy;
import iuh.fit.se.analytics.api.dto.AnalyticsSummaryResponse;
import iuh.fit.se.analytics.api.dto.RevenueDetailResponse;
import iuh.fit.se.shared.ai.client.dto.ForecastRequest;
import iuh.fit.se.shared.ai.client.dto.ForecastResponse;
import iuh.fit.se.shared.event.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public interface AnalyticsService {

    void recordEvent(DomainEvent event);

    AnalyticsSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate);

    RevenueDetailResponse getRevenueDetail(LocalDate fromDate, LocalDate toDate, GroupBy groupBy);

    ForecastResponse forecast(ForecastRequest request);

    // Cross-module query (used by menu module for trending)
    Map<Long, Long> getOrderCountsByItem(Instant from, Instant to);
}
