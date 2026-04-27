package iuh.fit.se.shared.ai;

import iuh.fit.se.shared.config.AiProperties;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class AiRuntimeState {

    private final AiProperties aiProperties;
    private final AtomicBoolean runtimeEnabled;
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicReference<Instant> lastHealthCheckedAt = new AtomicReference<>();
    private final AtomicReference<String> healthMessage = new AtomicReference<>("NOT_CHECKED");

    public AiRuntimeState(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.runtimeEnabled = new AtomicBoolean(aiProperties.isRuntimeEnabled());
    }

    public boolean canCall(AiOperation operation) {
        if (!aiProperties.isEnabled()) {
            return false;
        }
        if (!runtimeEnabled.get()) {
            return false;
        }
        if (operation == AiOperation.HEALTH) {
            return true;
        }
        return healthy.get();
    }

    public String shortCircuitReason(AiOperation operation) {
        if (!aiProperties.isEnabled()) {
            return "ai.enabled=false";
        }
        if (!runtimeEnabled.get()) {
            return "ai.runtime-enabled=false";
        }
        if (operation != AiOperation.HEALTH && !healthy.get()) {
            return "ai.health=UNHEALTHY";
        }
        return "unknown";
    }

    public void updateHealth(boolean healthy, String message) {
        this.healthy.set(healthy);
        this.healthMessage.set(message == null || message.isBlank() ? "UNKNOWN" : message);
        this.lastHealthCheckedAt.set(Instant.now());
    }

    public void setRuntimeEnabled(boolean enabled) {
        this.runtimeEnabled.set(enabled);
        if (!enabled) {
            updateHealth(false, "RUNTIME_DISABLED");
        }
    }

    public boolean isConfiguredEnabled() {
        return aiProperties.isEnabled();
    }

    public boolean isRuntimeEnabled() {
        return runtimeEnabled.get();
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    public boolean isAvailable() {
        return aiProperties.isEnabled() && runtimeEnabled.get() && healthy.get();
    }

    public Instant getLastHealthCheckedAt() {
        return lastHealthCheckedAt.get();
    }

    public String getHealthMessage() {
        return healthMessage.get();
    }
}
