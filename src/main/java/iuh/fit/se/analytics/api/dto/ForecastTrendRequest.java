package iuh.fit.se.analytics.api.dto;

import iuh.fit.se.shared.ai.client.dto.ForecastRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForecastTrendRequest(
        @NotBlank(message = "metric is required")
        String metric,

        @NotNull(message = "horizonDays is required")
        @Min(value = 1, message = "horizonDays must be at least 1")
        @Max(value = 90, message = "horizonDays must be at most 90")
        Integer horizonDays
) {
    public ForecastRequest toAiRequest() {
        return new ForecastRequest(metric.trim(), horizonDays);
    }
}
