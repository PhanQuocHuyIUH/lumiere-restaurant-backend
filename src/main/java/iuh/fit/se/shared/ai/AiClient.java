package iuh.fit.se.shared.ai;

import iuh.fit.se.shared.config.AiProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class AiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiClient.class);
    private static final String AI_SERVICE_KEY_HEADER = "X-AI-Service-Key";
    private static final int MAX_ATTEMPTS = 3;

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final AiProperties aiProperties;
    private final AiRuntimeState aiRuntimeState;

    public AiClient(AiProperties aiProperties, AiRuntimeState aiRuntimeState) {
        this.aiProperties = aiProperties;
        this.aiRuntimeState = aiRuntimeState;

        WebClient.Builder builder = WebClient.builder().baseUrl(aiProperties.getBaseUrl());
        if (hasText(aiProperties.getServiceKey())) {
            builder.defaultHeader(AI_SERVICE_KEY_HEADER, aiProperties.getServiceKey());
        }
        this.webClient = builder.build();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(20)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .recordExceptions(
                        TimeoutException.class,
                        WebClientRequestException.class,
                        WebClientResponseException.class,
                        RuntimeException.class
                )
                .build();

        this.circuitBreaker = CircuitBreaker.of("ai-service", circuitBreakerConfig);
    }

    public <T> Optional<T> post(String path, Object body, Class<T> responseType, AiOperation operation) {
        Duration timeout = resolveTimeout(operation);
        Supplier<Optional<T>> callSupplier = () -> webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(responseType)
                .timeout(timeout)
                .blockOptional();

        return executeWithResilience(path, operation, callSupplier);
    }

    public <T> Optional<T> get(String path, Class<T> responseType, AiOperation operation) {
        Duration timeout = resolveTimeout(operation);
        Supplier<Optional<T>> callSupplier = () -> webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .bodyToMono(responseType)
                .timeout(timeout)
                .blockOptional();

        return executeWithResilience(path, operation, callSupplier);
    }

    private <T> Optional<T> executeWithResilience(
            String path,
            AiOperation operation,
            Supplier<Optional<T>> callSupplier
    ) {
        if (!aiRuntimeState.canCall(operation)) {
            LOGGER.debug("AI call short-circuited: operation={}, path={}, reason={}",
                    operation,
                    path,
                    aiRuntimeState.shortCircuitReason(operation));
            return Optional.empty();
        }

        Supplier<Optional<T>> retrySupplier = () -> executeWithRetry(path, operation, callSupplier);
        Supplier<Optional<T>> protectedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retrySupplier);

        try {
            return protectedSupplier.get();
        } catch (CallNotPermittedException ex) {
            LOGGER.warn("AI circuit is open. Fallback applied: operation={}, path={}", operation, path);
            return Optional.empty();
        } catch (RuntimeException ex) {
            LOGGER.warn("AI call failed. Fallback applied: operation={}, path={}, reason={}",
                    operation,
                    path,
                    safeMessage(ex));
            return Optional.empty();
        }
    }

    private <T> Optional<T> executeWithRetry(
            String path,
            AiOperation operation,
            Supplier<Optional<T>> callSupplier
    ) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callSupplier.get();
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt < MAX_ATTEMPTS) {
                    LOGGER.warn("AI call attempt {}/{} failed. Retrying: operation={}, path={}, reason={}",
                            attempt,
                            MAX_ATTEMPTS,
                            operation,
                            path,
                            safeMessage(ex));
                }
            }
        }

        throw lastException == null ? new IllegalStateException("AI call failed without captured exception") : lastException;
    }

    private Duration resolveTimeout(AiOperation operation) {
        AiProperties.TimeoutMs timeout = aiProperties.getTimeoutMs();
        return switch (operation) {
            case RECOMMEND -> Duration.ofMillis(timeout.getRecommend());
            case FORECAST -> Duration.ofMillis(timeout.getForecast());
            case ANOMALY -> Duration.ofMillis(timeout.getAnomaly());
            case CHATBOT -> Duration.ofMillis(timeout.getChatbot());
            case BATCHING -> Duration.ofMillis(timeout.getBatching());
            case FEEDBACK -> Duration.ofMillis(timeout.getAnomaly());
            case HEALTH -> Duration.ofMillis(timeout.getRecommend());
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeMessage(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
                ? "unknown"
                : throwable.getMessage();
    }
}
