package iuh.fit.se.shared.ai.client.dto;

import java.util.List;

public record FeedbackRequest(
        List<FeedbackEventItem> events
) {}
