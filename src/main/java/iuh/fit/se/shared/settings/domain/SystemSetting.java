package iuh.fit.se.shared.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(name = "key", length = 100)
    private String key;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected SystemSetting() {}

    public static SystemSetting create(String key) {
        SystemSetting s = new SystemSetting();
        s.key = key;
        return s;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getUpdatedBy() { return updatedBy; }

    public void setValue(String value, Long staffId) {
        this.value = value;
        this.updatedAt = Instant.now();
        this.updatedBy = staffId;
    }
}
