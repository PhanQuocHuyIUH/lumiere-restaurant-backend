package iuh.fit.se.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled = true;
    private boolean runtimeEnabled = true;
    private String baseUrl = "http://localhost:8001";
    private String serviceKey = "";

    @Valid
    private TimeoutMs timeoutMs = new TimeoutMs();

    @Valid
    private HealthCheck healthCheck = new HealthCheck();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRuntimeEnabled() {
        return runtimeEnabled;
    }

    public void setRuntimeEnabled(boolean runtimeEnabled) {
        this.runtimeEnabled = runtimeEnabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public TimeoutMs getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(TimeoutMs timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    public static class TimeoutMs {

        @Min(100)
        private int recommend = 500;

        @Min(100)
        private int forecast = 2000;

        @Min(100)
        private int anomaly = 1000;

        @Min(100)
        private int chatbot = 1500;

        @Min(100)
        private int batching = 1200;

        public int getRecommend() {
            return recommend;
        }

        public void setRecommend(int recommend) {
            this.recommend = recommend;
        }

        public int getForecast() {
            return forecast;
        }

        public void setForecast(int forecast) {
            this.forecast = forecast;
        }

        public int getAnomaly() {
            return anomaly;
        }

        public void setAnomaly(int anomaly) {
            this.anomaly = anomaly;
        }

        public int getChatbot() {
            return chatbot;
        }

        public void setChatbot(int chatbot) {
            this.chatbot = chatbot;
        }

        public int getBatching() {
            return batching;
        }

        public void setBatching(int batching) {
            this.batching = batching;
        }
    }

    public static class HealthCheck {

        private boolean enabled = true;

        @Min(100)
        private long initialDelayMs = 5000;

        @Min(100)
        private long fixedDelayMs = 15000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }
    }
}
