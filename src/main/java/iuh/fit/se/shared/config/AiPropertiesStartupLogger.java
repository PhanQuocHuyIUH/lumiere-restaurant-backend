package iuh.fit.se.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AiPropertiesStartupLogger implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiPropertiesStartupLogger.class);

    private final AiProperties aiProperties;

    public AiPropertiesStartupLogger(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        AiProperties.TimeoutMs timeoutMs = aiProperties.getTimeoutMs();
        AiProperties.HealthCheck healthCheck = aiProperties.getHealthCheck();
        boolean serviceKeyConfigured = aiProperties.getServiceKey() != null
                && !aiProperties.getServiceKey().isBlank();

        LOGGER.info(
                "AI properties loaded: enabled={}, runtimeEnabled={}, baseUrl={}, serviceKeyConfigured={}, timeoutMs={recommend:{}, forecast:{}, anomaly:{}, chatbot:{}, batching:{}}, healthCheck={enabled:{}, initialDelayMs:{}, fixedDelayMs:{}}",
                aiProperties.isEnabled(),
                aiProperties.isRuntimeEnabled(),
                aiProperties.getBaseUrl(),
                serviceKeyConfigured,
                timeoutMs.getRecommend(),
                timeoutMs.getForecast(),
                timeoutMs.getAnomaly(),
                timeoutMs.getChatbot(),
                timeoutMs.getBatching(),
                healthCheck.isEnabled(),
                healthCheck.getInitialDelayMs(),
                healthCheck.getFixedDelayMs()
        );
    }
}
