package iuh.fit.se.analytics.application;

import iuh.fit.se.analytics.api.dto.AnalyticsSummaryResponse;
import iuh.fit.se.shared.event.DomainEvent;
import java.time.LocalDate;

public interface AnalyticsService {

    void recordEvent(DomainEvent event);

    AnalyticsSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate);
}