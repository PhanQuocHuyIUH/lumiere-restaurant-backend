package iuh.fit.se.ai;

import iuh.fit.se.ai.config.AiProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiHealthCheckScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiHealthCheckScheduler.class);

    private final AiClient aiClient;
    private final AiRuntimeState aiRuntimeState;
    private final AiProperties aiProperties;

    public AiHealthCheckScheduler(AiClient aiClient, AiRuntimeState aiRuntimeState, AiProperties aiProperties) {
        this.aiClient = aiClient;
        this.aiRuntimeState = aiRuntimeState;
        this.aiProperties = aiProperties;
    }

    @Async("aiTaskExecutor")
    @Scheduled(
            initialDelayString = "${ai.health-check.initial-delay-ms:10000}",
            fixedDelayString = "${ai.health-check.fixed-delay-ms:30000}"
    )
    public void refreshHealthStatus() {
        if (!aiProperties.getHealthCheck().isEnabled()) {
            return;
        }

        if (!aiRuntimeState.isConfiguredEnabled()) {
            aiRuntimeState.updateHealth(false, "AI_DISABLED");
            return;
        }

        if (!aiRuntimeState.isRuntimeEnabled()) {
            aiRuntimeState.updateHealth(false, "RUNTIME_DISABLED");
            return;
        }

        boolean previous = aiRuntimeState.isHealthy();
        Optional<String> response = aiClient.get("/ai/health", String.class, AiOperation.HEALTH);
        boolean current = response.isPresent();

        aiRuntimeState.updateHealth(current, current ? "UP" : "NO_RESPONSE");

        if (previous != current) {
            if (current) {
                LOGGER.info("AI health recovered");
            } else {
                LOGGER.warn("AI health degraded; fallback mode enabled");
            }
        }
    }
}
