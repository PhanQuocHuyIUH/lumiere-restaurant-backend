package iuh.fit.se.analytics.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.analytics.api.GroupBy;
import iuh.fit.se.analytics.api.dto.AnalyticsSummaryResponse;
import iuh.fit.se.analytics.api.dto.RevenueDetailResponse;
import iuh.fit.se.analytics.api.dto.RevenuePeriodEntry;
import iuh.fit.se.analytics.api.dto.TopMenuItemEntry;
import iuh.fit.se.analytics.application.AnalyticsService;
import iuh.fit.se.analytics.domain.OrderEvent;
import iuh.fit.se.analytics.repository.OrderEventRepository;
import iuh.fit.se.ai.AiClient;
import iuh.fit.se.ai.AiOperation;
import iuh.fit.se.ai.client.dto.ForecastRequest;
import iuh.fit.se.ai.client.dto.ForecastResponse;
import iuh.fit.se.shared.event.DomainEvent;
import iuh.fit.se.shared.event.KitchenTaskDoneEvent;
import iuh.fit.se.shared.event.OrderCancelledEvent;
import iuh.fit.se.shared.event.OrderConfirmedEvent;
import iuh.fit.se.shared.event.OrderCreatedEvent;
import iuh.fit.se.shared.event.PaymentFailedEvent;
import iuh.fit.se.shared.event.PaymentSuccessEvent;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private static final ZoneId DATABASE_ZONE = ZoneOffset.UTC;

    private final OrderEventRepository orderEventRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public AnalyticsServiceImpl(OrderEventRepository orderEventRepository, AiClient aiClient, ObjectMapper objectMapper) {
        this.orderEventRepository = orderEventRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void recordEvent(DomainEvent event) {
        if (event == null) {
            return;
        }

        Optional<Long> orderIdOpt = extractOrderId(event);
        if (orderIdOpt.isEmpty()) {
            LOGGER.warn("Skipping analytics event {} because orderId is missing", event.getClass().getSimpleName());
            return;
        }

        EventActor actor = resolveActor();
        OrderEvent orderEvent = OrderEvent.capture(
                orderIdOpt.get(),
                toEventType(event),
                resolveEventSource(event),
                actor.actorId(),
                actor.actorType(),
                toMetadata(event),
                event.getOccurredOn()
        );
        orderEventRepository.save(orderEvent);
    }

    @Override
    public AnalyticsSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay(DATABASE_ZONE).toInstant();
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay(DATABASE_ZONE).toInstant();

        OrderEventRepository.AnalyticsSummaryProjection summary = orderEventRepository.summarize(fromTime, toTime);
        return new AnalyticsSummaryResponse(
                normalizeCount(summary == null ? null : summary.getTotalOrders()),
                normalizeCount(summary == null ? null : summary.getConfirmedOrders()),
                normalizeCount(summary == null ? null : summary.getCancelledOrders()),
                normalizeAmount(summary == null ? null : summary.getTotalRevenue()),
                normalizeAmount(summary == null ? null : summary.getTotalNetRevenue()),
                normalizeAmount(summary == null ? null : summary.getTotalTax()),
                normalizeCount(summary == null ? null : summary.getSuccessfulPayments()),
                normalizeCount(summary == null ? null : summary.getFailedPayments()),
                fromDate,
                toDate,
                Instant.now()
        );
    }

    @Override
    public RevenueDetailResponse getRevenueDetail(LocalDate fromDate, LocalDate toDate, GroupBy groupBy) {
        validateDateRange(fromDate, toDate);

        GroupBy effectiveGroupBy = groupBy == null ? GroupBy.DAY : groupBy;
        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay(DATABASE_ZONE).toInstant();
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay(DATABASE_ZONE).toInstant();

        String granularity = toPostgresGranularity(effectiveGroupBy);

        // --- Period breakdown ---
        List<OrderEventRepository.RevenuePeriodProjection> periodRows =
                orderEventRepository.findRevenueByPeriod(granularity, fromTime, toTime);

        List<RevenuePeriodEntry> periods = periodRows.stream()
                .map(row -> new RevenuePeriodEntry(
                        formatPeriodLabel(row.getPeriod(), effectiveGroupBy),
                        normalizeAmount(row.getRevenue()),
                        normalizeAmount(row.getNetRevenue()),
                        normalizeAmount(row.getTaxAmount()),
                        normalizeCount(row.getOrderCount())
                ))
                .toList();

        BigDecimal totalRevenue = periods.stream()
                .map(RevenuePeriodEntry::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetRevenue = periods.stream()
                .map(RevenuePeriodEntry::netRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTax = periods.stream()
                .map(RevenuePeriodEntry::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalOrders = periods.stream()
                .mapToLong(RevenuePeriodEntry::orderCount)
                .sum();

        // --- Top 10 best-seller ---
        List<OrderEventRepository.TopMenuItemProjection> topRows =
                orderEventRepository.findTop10MenuItems(fromTime, toTime);

        List<TopMenuItemEntry> topItems = topRows.stream()
                .map(row -> new TopMenuItemEntry(
                        row.getMenuItemId(),
                        row.getMenuItemName(),
                        normalizeCount(row.getTotalQuantity()),
                        normalizeCount(row.getOrderCount()),
                        normalizeAmount(row.getTotalRevenue())
                ))
                .toList();

        return new RevenueDetailResponse(
                fromDate,
                toDate,
                effectiveGroupBy.name(),
                totalRevenue,
                totalNetRevenue,
                totalTax,
                totalOrders,
                periods,
                topItems,
                Instant.now()
        );
    }

    @Override
    public ForecastResponse forecast(ForecastRequest request) {
        ForecastRequest safeRequest = normalizeForecastRequest(request);
        return aiClient.post("/ai/forecast", safeRequest, ForecastResponse.class, AiOperation.FORECAST)
                .orElseGet(() -> new ForecastResponse(false, safeRequest.metric(), List.of()));
    }

    private String toPostgresGranularity(GroupBy groupBy) {
        return switch (groupBy) {
            case DAY   -> "day";
            case WEEK  -> "week";
            case MONTH -> "month";
            case YEAR  -> "year";
        };
    }

    private String formatPeriodLabel(Instant period, GroupBy groupBy) {
        if (period == null) {
            return "";
        }
        LocalDate date = period.atZone(DATABASE_ZONE).toLocalDate();
        return switch (groupBy) {
            case DAY   -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);          // 2025-04-30
            case WEEK  -> date.getYear() + "-W"
                    + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)); // 2025-W17
            case MONTH -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));    // 2025-04
            case YEAR  -> String.valueOf(date.getYear());                          // 2025
        };
    }

    private ForecastRequest normalizeForecastRequest(ForecastRequest request) {
        if (request == null) {
            throw new DomainException("Forecast request is required");
        }

        String metric = normalizeKeyword(request.metric());
        if (metric == null) {
            throw new DomainException("metric is required");
        }

        int horizonDays = request.horizonDays();
        if (horizonDays < 1) {
            throw new DomainException("horizonDays must be greater than 0");
        }

        return new ForecastRequest(metric.toLowerCase(Locale.ROOT), horizonDays);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new DomainException("fromDate must be before or equal to toDate");
        }
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Optional<Long> extractOrderId(DomainEvent event) {
        if (event instanceof OrderCreatedEvent createdEvent) {
            return Optional.ofNullable(createdEvent.getOrderId());
        }
        if (event instanceof OrderConfirmedEvent confirmedEvent) {
            return Optional.ofNullable(confirmedEvent.getOrderId());
        }
        if (event instanceof OrderCancelledEvent cancelledEvent) {
            return Optional.ofNullable(cancelledEvent.getOrderId());
        }
        if (event instanceof PaymentSuccessEvent paymentSuccessEvent) {
            return Optional.ofNullable(paymentSuccessEvent.getOrderId());
        }
        if (event instanceof PaymentFailedEvent paymentFailedEvent) {
            return Optional.ofNullable(paymentFailedEvent.getOrderId());
        }
        if (event instanceof KitchenTaskDoneEvent kitchenTaskDoneEvent) {
            return Optional.ofNullable(kitchenTaskDoneEvent.getOrderId());
        }

        try {
            Method getOrderId = event.getClass().getMethod("getOrderId");
            Object orderIdValue = getOrderId.invoke(event);
            if (orderIdValue instanceof Number number) {
                return Optional.of(number.longValue());
            }
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private String resolveEventSource(DomainEvent event) {
        if (event instanceof OrderCreatedEvent
                || event instanceof OrderConfirmedEvent
                || event instanceof OrderCancelledEvent) {
            return "ORDERING";
        }
        if (event instanceof PaymentSuccessEvent || event instanceof PaymentFailedEvent) {
            return "BILLING";
        }
        if (event instanceof KitchenTaskDoneEvent) {
            return "KITCHEN";
        }

        String name = event.getClass().getSimpleName().toUpperCase(Locale.ROOT);
        if (name.contains("SERV")) {
            return "SERVING";
        }
        if (name.contains("BATCH") || name.contains("KITCHEN")) {
            return "KITCHEN";
        }
        if (name.contains("PAY")) {
            return "BILLING";
        }
        if (name.contains("ORDER")) {
            return "ORDERING";
        }

        return "SYSTEM";
    }

    private String toEventType(DomainEvent event) {
        String className = event.getClass().getSimpleName();
        String baseName = className.endsWith("Event")
                ? className.substring(0, className.length() - "Event".length())
                : className;
        return baseName
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> toMetadata(DomainEvent event) {
        Map<String, Object> metadata = objectMapper.convertValue(event, new TypeReference<>() {
        });

        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        } else if (!(metadata instanceof LinkedHashMap)) {
            metadata = new LinkedHashMap<>(metadata);
        }

        metadata.putIfAbsent("eventClass", event.getClass().getSimpleName());
        metadata.putIfAbsent("eventId", event.getEventId());
        metadata.putIfAbsent("occurredOn", event.getOccurredOn() == null ? null : event.getOccurredOn().toString());
        return metadata;
    }

    private EventActor resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new EventActor(null, "SYSTEM");
        }

        if (authentication.getPrincipal() instanceof JwtPrincipal principal && principal.getStaffId() != null) {
            return new EventActor(principal.getStaffId(), "STAFF");
        }

        return new EventActor(null, "SYSTEM");
    }

    private Long normalizeCount(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getOrderCountsByItem(Instant from, Instant to) {
        return orderEventRepository.findOrderCountsByItem(from, to).stream()
                .collect(java.util.stream.Collectors.toMap(
                        OrderEventRepository.ItemOrderCountProjection::getMenuItemId,
                        OrderEventRepository.ItemOrderCountProjection::getOrderCount
                ));
    }

    private record EventActor(Long actorId, String actorType) {
    }
}
