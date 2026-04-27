package iuh.fit.se.analytics.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.analytics.api.dto.AnalyticsSummaryResponse;
import iuh.fit.se.analytics.application.AnalyticsService;
import iuh.fit.se.analytics.domain.OrderEvent;
import iuh.fit.se.analytics.infrastructure.OrderEventRepository;
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
import java.util.LinkedHashMap;
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
    private final ObjectMapper objectMapper;

    public AnalyticsServiceImpl(OrderEventRepository orderEventRepository, ObjectMapper objectMapper) {
        this.orderEventRepository = orderEventRepository;
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
                normalizeCount(summary == null ? null : summary.getSuccessfulPayments()),
                normalizeCount(summary == null ? null : summary.getFailedPayments()),
                fromDate,
                toDate,
                Instant.now()
        );
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new DomainException("fromDate must be before or equal to toDate");
        }
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

    private record EventActor(Long actorId, String actorType) {
    }
}