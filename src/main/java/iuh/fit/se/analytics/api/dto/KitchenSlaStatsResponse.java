package iuh.fit.se.analytics.api.dto;

public record KitchenSlaStatsResponse(
        long totalTasks,
        long breachedTasks,
        double breachRate,
        double avgWaitSeconds,
        long p95WaitSeconds,
        long maxWaitSeconds
) {
}
