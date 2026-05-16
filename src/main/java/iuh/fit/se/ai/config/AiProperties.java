package iuh.fit.se.ai.config;

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

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isRuntimeEnabled() { return runtimeEnabled; }
    public void setRuntimeEnabled(boolean runtimeEnabled) { this.runtimeEnabled = runtimeEnabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }

    public TimeoutMs getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(TimeoutMs timeoutMs) { this.timeoutMs = timeoutMs; }

    public HealthCheck getHealthCheck() { return healthCheck; }
    public void setHealthCheck(HealthCheck healthCheck) { this.healthCheck = healthCheck; }

    public static class TimeoutMs {

        @Min(100) private int syncMenu      = 5_000;
        @Min(100) private int recommend     = 5_000;
        @Min(100) private int chatbot       = 25_000;  // Gemini API p95 ~8s + buffer
        @Min(100) private int forecast      = 10_000;
        @Min(100) private int comboGenerate = 15_000;
        @Min(100) private int batching      = 5_000;
        @Min(100) private int feedback      = 3_000;
        @Min(100) private int retrain       = 5_000;   // chỉ trigger, chạy background
        @Min(100) private int jobStatus     = 3_000;

        public int getSyncMenu() { return syncMenu; }
        public void setSyncMenu(int syncMenu) { this.syncMenu = syncMenu; }

        public int getRecommend() { return recommend; }
        public void setRecommend(int recommend) { this.recommend = recommend; }

        public int getChatbot() { return chatbot; }
        public void setChatbot(int chatbot) { this.chatbot = chatbot; }

        public int getForecast() { return forecast; }
        public void setForecast(int forecast) { this.forecast = forecast; }

        public int getComboGenerate() { return comboGenerate; }
        public void setComboGenerate(int comboGenerate) { this.comboGenerate = comboGenerate; }

        public int getBatching() { return batching; }
        public void setBatching(int batching) { this.batching = batching; }

        public int getFeedback() { return feedback; }
        public void setFeedback(int feedback) { this.feedback = feedback; }

        public int getRetrain() { return retrain; }
        public void setRetrain(int retrain) { this.retrain = retrain; }

        public int getJobStatus() { return jobStatus; }
        public void setJobStatus(int jobStatus) { this.jobStatus = jobStatus; }
    }

    public static class HealthCheck {

        private boolean enabled = true;
        @Min(100) private long initialDelayMs = 5000;
        @Min(100) private long fixedDelayMs = 15000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }

        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
    }
}
