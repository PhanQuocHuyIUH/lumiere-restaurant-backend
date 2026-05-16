package iuh.fit.se.ai.client.dto;

import java.util.List;

public record FeedbackRequest(
        List<FeedbackEventItem> events
) {}
