package iuh.fit.se.support.domain;

import java.time.Instant;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "support_requests", schema = "support")
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String tableCode;

    private String qrSessionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private SupportRequestStatus status = SupportRequestStatus.CREATED;

    private Long assignedToStaffId;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public SupportRequest() {}

    public SupportRequest(String message, String tableCode, String qrSessionId) {
        this.message = message;
        this.tableCode = tableCode;
        this.qrSessionId = qrSessionId;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getTableCode() {
        return tableCode;
    }

    public String getQrSessionId() {
        return qrSessionId;
    }

    public SupportRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SupportRequestStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Long getAssignedToStaffId() {
        return assignedToStaffId;
    }

    public void setAssignedToStaffId(Long assignedToStaffId) {
        this.assignedToStaffId = assignedToStaffId;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

}
