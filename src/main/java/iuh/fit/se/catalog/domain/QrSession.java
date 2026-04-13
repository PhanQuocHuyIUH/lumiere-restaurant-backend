package iuh.fit.se.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "qr_sessions", schema = "table_mgmt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QrSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "qr_code_id", nullable = false)
    private Long qrCodeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "issued_to", length = 120)
    private String issuedTo;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "qr_session_status_enum", nullable = false)
    @Builder.Default
    private QrSessionStatus status = QrSessionStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_access_at")
    private Instant lastAccessAt;

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = QrSessionStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.status == QrSessionStatus.ACTIVE && this.expiresAt != null && this.expiresAt.isBefore(Instant.now())) {
            this.status = QrSessionStatus.EXPIRED;
            this.revokedAt = Instant.now();
        }
    }

    public static QrSession issue(String sessionId, Long qrCodeId, Long tableId, Instant expiresAt, String issuedTo) {
        return QrSession.builder()
                .sessionId(sessionId)
                .qrCodeId(qrCodeId)
                .tableId(tableId)
                .expiresAt(expiresAt)
                .issuedTo(issuedTo)
                .status(QrSessionStatus.ACTIVE)
                .build();
    }

    public boolean isActive(Instant now) {
        return this.status == QrSessionStatus.ACTIVE && this.expiresAt != null && this.expiresAt.isAfter(now);
    }

    public void markExpired() {
        this.status = QrSessionStatus.EXPIRED;
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public void revoke() {
        this.status = QrSessionStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    public void touchAccess() {
        this.lastAccessAt = Instant.now();
    }
}
